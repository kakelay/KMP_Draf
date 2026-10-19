package org.example.project.parking

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.example.project.parking.data.remote.*
import org.example.project.parking.data.repository.ApiParkingRepository
import org.example.project.parking.domain.*
import kotlin.test.*

class ApiParkingRepositoryTest {
    @Test fun repositoryMapsHttpFailureToDomainFailure() = runTest {
        val client = HttpClient(MockEngine {
            respond("""{"responseCode":"AUTH","message":"expired","referenceNumber":"ref-401"}""", HttpStatusCode.Unauthorized)
        })
        try {
            val repository = ApiParkingRepository(JomnotApi(client, "https://example.test", FirebaseTokenProvider { "expired" }))
            val failure = assertFailsWith<ParkingException> { repository.search(ParkingSearch()) }
            assertEquals(ParkingFailureReason.SESSION_EXPIRED, failure.reason)
            assertEquals("ref-401", failure.referenceNumber)
        } finally { client.close() }
    }

    @Test fun repositoryDoesNotConvertCancellationIntoUiError() = runTest {
        val client = HttpClient(MockEngine { error("Token lookup should cancel first") })
        try {
            val api = JomnotApi(client, "https://example.test", FirebaseTokenProvider { throw CancellationException("cancelled") })
            assertFailsWith<CancellationException> { ApiParkingRepository(api).search(ParkingSearch()) }
        } finally { client.close() }
    }
}
