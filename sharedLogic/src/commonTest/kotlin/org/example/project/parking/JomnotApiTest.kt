package org.example.project.parking

import org.example.project.parking.data.remote.*
import org.example.project.parking.domain.ParkingSearch

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class JomnotApiTest {
    @Test fun searchEncodesParametersAndUsesCurrentFirebaseToken() = runTest {
        var token: String? = "first-token"
        val authorizations = mutableListOf<String?>()
        val engine = MockEngine { request ->
            assertEquals("/v1/parking/locations", request.url.encodedPath)
            assertEquals("mall", request.url.parameters["category"])
            assertEquals("A & B", request.url.parameters["q"])
            assertEquals("0", request.url.parameters["page"])
            assertEquals("20", request.url.parameters["size"])
            authorizations += request.headers[HttpHeaders.Authorization]
            respond("""{"responseCode":"00","message":"ok","data":{"data":[{"id":"p1","name":"Mall","address":"Street 1","latitude":11.5,"longitude":104.9,"pricePerHour":1.25,"availableSpaces":8,"isFavorite":true,"extraField":"ignored"}],"count":21,"page":0,"size":20}}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine)
        try {
            val api = JomnotApi(client, "https://example.test/", FirebaseTokenProvider { token })
            val page = api.search(ParkingSearch(query = "A & B", category = "mall"))
            assertEquals("1.25", page.locations.single().pricePerHour)
            assertTrue(page.locations.single().isFavorite)
            assertTrue(page.hasNext)
            token = "refreshed-token"
            api.search(ParkingSearch(query = "A & B", category = "mall"))
            token = null
            api.search(ParkingSearch(query = "A & B", category = "mall"))
            assertEquals(listOf("Bearer first-token", "Bearer refreshed-token", null), authorizations)
        } finally { client.close() }
    }

    @Test fun signedOutCustomerRequestNeverReachesNetwork() = runTest {
        val client = HttpClient(MockEngine { error("Must not send an unauthenticated customer request") })
        try {
            val api = JomnotApi(client, "https://example.test", FirebaseTokenProvider { null })
            assertFailsWith<SignInRequired> { api.account() }
        } finally { client.close() }
    }

    @Test fun deleteAcceptsSuccessWithoutData() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Delete, request.method)
            assertEquals("/v1/customer/favorites/p1", request.url.encodedPath)
            respond("""{"responseCode":"00","message":"removed"}""")
        })
        try { JomnotApi(client, "https://example.test", FirebaseTokenProvider { "token" }).removeFavorite("p1") }
        finally { client.close() }
    }

    @Test fun apiFailurePreservesSupportReference() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"responseCode":"AUTH","message":"denied","referenceNumber":"ref-123"}""", HttpStatusCode.Forbidden)
        })
        try {
            val failure = assertFailsWith<ApiException> {
                JomnotApi(client, "https://example.test", FirebaseTokenProvider { "token" }).account()
            }
            assertEquals(403, failure.httpStatus)
            assertEquals("ref-123", failure.referenceNumber)
        } finally { client.close() }
    }

    @Test fun logsRequestAndFailedResponseWithoutCredentials() = runTest {
        val messages = mutableListOf<String>()
        val client = HttpClient(MockEngine {
            respond("""{"responseCode":"AUTH","message":"denied","accessToken":"sensitive","data":{"password":"private"}}""",
                HttpStatusCode.Forbidden)
        })
        try {
            val api = JomnotApi(client, "https://example.test", FirebaseTokenProvider { "firebase-secret" }, messages::add)
            assertFailsWith<ApiException> { api.account() }
            assertEquals(2, messages.size)
            assertContains(messages[0], "REQUEST GET https://example.test/v1/customer/account")
            assertContains(messages[0], "Body: <empty>")
            assertContains(messages[1], "Status: 403")
            assertContains(messages[1], "denied")
            assertFalse(messages.joinToString().contains("sensitive"))
            assertFalse(messages.joinToString().contains("private"))
            assertFalse(messages.joinToString().contains("firebase-secret"))
        } finally { client.close() }
    }

    @Test fun malformedResponseIsOmittedFromLogs() {
        assertEquals("<non-JSON body omitted>", redactApiBody("password=private", null))
    }

    @Test fun rejectsInvalidSearchBeforeNetwork() {
        assertFailsWith<IllegalArgumentException> { ParkingSearch(page = -1) }
        assertFailsWith<IllegalArgumentException> { ParkingSearch(size = 101) }
        assertFailsWith<IllegalArgumentException> { ParkingSearch(latitude = Double.NaN) }
    }
}
