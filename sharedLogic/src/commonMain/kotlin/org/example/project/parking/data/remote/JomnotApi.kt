package org.example.project.parking.data.remote

import org.example.project.parking.domain.ParkingLocation
import org.example.project.parking.domain.ParkingPage
import org.example.project.parking.domain.ParkingSearch

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.decodeFromJsonElement

/** Ask the Firebase SDK for a current ID token on every request. Null means signed out.
 * The SDK adapter should refresh expired tokens; do not persist a copied token here.
 */
fun interface FirebaseTokenProvider { suspend fun idToken(): String? }

class ApiException(
    val httpStatus: Int, val responseCode: String?, val referenceNumber: String?,
    message: String,
) : Exception(message)
class SignInRequired : Exception("Sign in with Firebase to use this feature.")

fun createParkingHttpClient() = HttpClient {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 20_000
    }
}

/** Application owns the client. Supply a logger only for development builds. */
class JomnotApi(
    private val client: HttpClient,
    baseUrl: String,
    private val tokens: FirebaseTokenProvider,
    private val logger: ((String) -> Unit)? = null,
) {
    private val base = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }
    init {
        val url = Url(base)
        require(url.protocol == URLProtocol.HTTPS || url.protocol == URLProtocol.HTTP)
        require(url.host.isNotBlank() && url.user.isNullOrEmpty() && url.password.isNullOrEmpty())
        require(url.parameters.isEmpty() && url.fragment.isEmpty())
    }

    private suspend inline fun <reified T> call(
        path: String, method: HttpMethod = HttpMethod.Get, authenticated: Boolean = false,
        query: Map<String, String> = emptyMap(),
    ): T? {
        val token = tokens.idToken()?.takeIf { it.isNotBlank() }
        if (authenticated && token == null) throw SignInRequired()
        val requestUrl = URLBuilder("$base/v1/$path").apply {
            query.forEach { (key, value) -> parameters.append(key, value) }
        }.buildString()
        logApi("REQUEST ${method.value} $requestUrl\nBody: <empty>")
        val response = client.request("$base/v1/$path") {
            this.method = method
            accept(ContentType.Application.Json)
            token?.let { bearerAuth(it) }
            url { query.forEach { (key, value) -> parameters.append(key, value) } }
        }
        val body = response.bodyAsText()
        if (logger != null) {
            logApi("RESPONSE ${method.value} $requestUrl\nStatus: ${response.status.value}\nBody: ${redactApiBody(body, token)}")
        }
        val envelope = try { json.decodeFromString<ApiEnvelope<JsonElement>>(body) }
        catch (error: kotlinx.serialization.SerializationException) {
            throw ApiException(response.status.value, null, null, "Unexpected API response (${response.status.value}).")
        }
        if (response.status.value !in 200..299 || envelope.responseCode != "00") {
            throw ApiException(response.status.value, envelope.responseCode, envelope.referenceNumber, envelope.message)
        }
        return envelope.data?.let { json.decodeFromJsonElement<T>(it) }
    }
    private fun logApi(message: String) {
        // Logging failures must never change the result of an API call.
        try { logger?.invoke(message) } catch (_: Exception) { }
    }
    private fun <T> T?.required(): T = this ?: error("Successful API response is missing data")
    private fun segment(value: String): String {
        require(value.isNotBlank())
        return value.encodeURLPathPart()
    }
    private fun paging(page: Int, size: Int): Map<String, String> {
        require(page >= 0 && size in 1..100)
        return mapOf("page" to "$page", "size" to "$size")
    }
    suspend fun home(): AppHome = call<AppHome>("app/home").required()
    suspend fun categories(): List<ParkingCategory> = call<List<ParkingCategory>>("parking/categories").required()
    suspend fun search(search: ParkingSearch): ParkingPage {
        val query = paging(search.page, search.size).toMutableMap()
        search.category?.let { query["category"] = it }
        search.query.trim().takeIf { it.isNotEmpty() }?.let { query["q"] = it }
        search.latitude?.let { query["latitude"] = "$it" }
        search.longitude?.let { query["longitude"] = "$it" }
        return call<ParkingPageDto>("parking/locations", query = query).required().toDomain()
    }
    suspend fun location(id: String): ParkingLocation =
        call<ParkingDto>("parking/locations/${segment(id)}").required().toDomain()
    suspend fun account(): Account = call<Account>("customer/account", authenticated = true).required()
    suspend fun favorites(page: Int = 0, size: Int = 20): ParkingPage =
        call<ParkingPageDto>("customer/favorites", authenticated = true, query = paging(page, size)).required().toDomain()
    suspend fun customerFavorites(customerId: String, page: Int = 0, size: Int = 20): ParkingPage =
        call<ParkingPageDto>("customer/favorites/customer/${segment(customerId)}", authenticated = true,
            query = paging(page, size)).required().toDomain()
    suspend fun saveFavorite(id: String): ParkingLocation =
        call<ParkingDto>("customer/favorites/${segment(id)}", HttpMethod.Put, true).required().toDomain()
    suspend fun removeFavorite(id: String) { call<JsonElement>("customer/favorites/${segment(id)}", HttpMethod.Delete, true) }
    suspend fun zones(): List<CustomerZone> = call<List<CustomerZone>>("zone/customer/find-zones", authenticated = true).required()
    suspend fun selectZone(id: String): CustomerZone =
        call<CustomerZone>("zone/customer/selected-zone/${segment(id)}", HttpMethod.Put, true).required()
    suspend fun notifications(skip: Int = 0, take: Int = 20): NotificationPage {
        require(skip >= 0 && take in 1..100)
        return call<NotificationPage>("notifications", authenticated = true,
            query = mapOf("skip" to "$skip", "take" to "$take")).required()
    }
    suspend fun markRead(id: String): CustomerNotification =
        call<CustomerNotification>("notifications/${segment(id)}/read", HttpMethod.Put, true).required()
}

/** Keep credential fields out of debug output, including nested objects and arrays. */
internal fun redactApiBody(body: String, token: String?): String {
    if (body.isBlank()) return "<empty>"
    val json = Json { ignoreUnknownKeys = true }
    fun redact(element: kotlinx.serialization.json.JsonElement): kotlinx.serialization.json.JsonElement = when (element) {
        is kotlinx.serialization.json.JsonObject -> kotlinx.serialization.json.JsonObject(element.mapValues { (key, value) ->
            val normalized = key.lowercase().filter { it.isLetterOrDigit() }
            if (normalized.contains("token") || normalized.contains("password") ||
                normalized.contains("secret") || normalized in setOf("authorization", "cookie", "apikey"))
                kotlinx.serialization.json.JsonPrimitive("[REDACTED]")
            else redact(value)
        })
        is kotlinx.serialization.json.JsonArray -> kotlinx.serialization.json.JsonArray(element.map { redact(it) })
        else -> element
    }
    val safe = try {
        val parsed = json.parseToJsonElement(body)
        if (parsed !is kotlinx.serialization.json.JsonObject && parsed !is kotlinx.serialization.json.JsonArray)
            return "<non-JSON body omitted>"
        redact(parsed).toString()
    }
        catch (_: kotlinx.serialization.SerializationException) { return "<non-JSON body omitted>" }
    return if (token.isNullOrBlank()) safe else safe.replace(token, "[REDACTED]")
}
