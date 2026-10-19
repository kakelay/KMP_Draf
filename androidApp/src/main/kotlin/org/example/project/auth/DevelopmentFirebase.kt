package org.example.project.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import org.example.project.parking.data.remote.FirebaseTokenProvider

/** Native equivalent of the supplied Flutter DevelopmentFirebaseOptions.android. */
object DevelopmentFirebase {
    private const val APP_NAME = "jomnot-development"
    fun auth(context: Context): FirebaseAuth {
        val app = FirebaseApp.getApps(context).find { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setApiKey("AIzaSyBDWpPskYD28tzuaDpGn34quc5DUKwMZ1E")
                .setApplicationId("1:1014341937975:android:001fb8fd189cbe7a5948b2")
                .setGcmSenderId("1014341937975")
                .setProjectId("jomnot-merchan")
                .setStorageBucket("jomnot-merchan.firebasestorage.app")
                .build(), APP_NAME)
        return FirebaseAuth.getInstance(app)
    }
}

class AndroidFirebaseTokenProvider(private val auth: FirebaseAuth) : FirebaseTokenProvider {
    override suspend fun idToken(): String? {
        val user = auth.currentUser ?: return null
        // Firebase refreshes expired tokens. Failures propagate; do not silently drop authentication.
        val token = user.getIdToken(false).await().token
        check(!token.isNullOrBlank()) { "Firebase did not return an ID token" }
        return token
    }
}
