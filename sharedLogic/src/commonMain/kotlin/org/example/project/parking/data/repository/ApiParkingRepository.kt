package org.example.project.parking.data.repository

import kotlinx.coroutines.CancellationException
import org.example.project.parking.data.remote.ApiException
import org.example.project.parking.data.remote.JomnotApi
import org.example.project.parking.data.remote.SignInRequired
import org.example.project.parking.domain.*

class ApiParkingRepository(private val api: JomnotApi) : ParkingRepository {
    override suspend fun search(search: ParkingSearch): ParkingPage = execute { api.search(search) }
    override suspend fun setFavorite(id: String, favorite: Boolean): ParkingLocation? = execute {
        if (favorite) api.saveFavorite(id) else { api.removeFavorite(id); null }
    }

    private suspend fun <T> execute(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: SignInRequired) {
        throw ParkingException(ParkingFailureReason.SIGN_IN_REQUIRED, cause = error)
    } catch (error: ApiException) {
        val reason = when (error.httpStatus) {
            401 -> ParkingFailureReason.SESSION_EXPIRED
            403 -> ParkingFailureReason.ACCESS_DENIED
            else -> ParkingFailureReason.REQUEST_FAILED
        }
        throw ParkingException(reason, error.referenceNumber, error)
    } catch (error: Exception) {
        throw ParkingException(ParkingFailureReason.CONNECTION_FAILED, cause = error)
    }
}
