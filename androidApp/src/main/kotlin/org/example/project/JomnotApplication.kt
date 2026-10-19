package org.example.project

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import org.example.project.auth.AndroidFirebaseTokenProvider
import org.example.project.auth.DevelopmentFirebase
import org.example.project.parking.data.remote.*
import org.example.project.parking.data.repository.ApiParkingRepository
import org.example.project.parking.domain.ParkingRepository

class JomnotApplication : Application() {
    lateinit var firebaseAuth: FirebaseAuth
        private set
    lateinit var tokenProvider: AndroidFirebaseTokenProvider
        private set
    var parkingRepository: ParkingRepository? = null
        private set
    private var httpClient: io.ktor.client.HttpClient? = null

    override fun onCreate() {
        super.onCreate()
        firebaseAuth = DevelopmentFirebase.auth(this)
        tokenProvider = AndroidFirebaseTokenProvider(firebaseAuth)
        val baseUrl = BuildConfig.API_BASE_URL
        if (baseUrl.isNotBlank()) {
            check(BuildConfig.DEBUG || baseUrl.startsWith("https://")) { "Release API must use HTTPS" }
            val client = createParkingHttpClient()
            httpClient = client
            val apiLogger: ((String) -> Unit)? = if (BuildConfig.DEBUG) {
                { message ->
                    // Logcat truncates long entries; preserve the complete body in chunks.
                    message.chunked(3000).forEach { android.util.Log.d("JomnotApi", it) }
                }
            } else null
            parkingRepository = ApiParkingRepository(JomnotApi(client, baseUrl, tokenProvider, apiLogger))
        }
    }
}
