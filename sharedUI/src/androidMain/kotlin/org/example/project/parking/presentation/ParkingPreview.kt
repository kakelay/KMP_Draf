package org.example.project.parking.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun ParkingAndroidPreview() {
    MaterialTheme {
        ParkingScreen(
            state = ParkingUiState(),
            onQuery = {},
            onCategory = {},
            onSearch = {},
            onRetry = {},
            onLoadMore = {},
            onFavorite = {},
        )
    }
}