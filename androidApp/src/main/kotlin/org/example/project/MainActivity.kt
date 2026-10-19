package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val dependencies get() = application as JomnotApplication
    private var sessionUid by mutableStateOf<String?>(null)
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val newUid = auth.currentUser?.uid
        if (newUid != sessionUid) {
            // Clear cards and cancel requests belonging to the previous Firebase identity.
            viewModelStore.clear()
            sessionUid = newUid
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        sessionUid = dependencies.firebaseAuth.currentUser?.uid
        setContent {
            key(sessionUid) {
                App(parkingRepository = dependencies.parkingRepository)
            }
        }
    }
    override fun onStart() {
        super.onStart()
        dependencies.firebaseAuth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        dependencies.firebaseAuth.removeAuthStateListener(authListener)
        super.onStop()
    }
}
