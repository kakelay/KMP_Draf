package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.parking.domain.ParkingRepository
import org.example.project.parking.presentation.ParkingRoute

@Composable
fun App(parkingRepository: ParkingRepository?) {
    MaterialTheme {
        if (parkingRepository != null) {
            ParkingRoute(repository = parkingRepository)
        } else {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.safeDrawingPadding().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Parking is not configured. Set JOMNOT_API_BASE_URL to connect to the service.")
                }
            }
        }
    }
}
