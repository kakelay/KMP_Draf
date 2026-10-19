package org.example.project.parking.presentation

import org.example.project.parking.domain.ParkingLocation

data class ParkingUiState(
    val query: String = "", val category: String = "all",
    val locations: List<ParkingLocation> = emptyList(),
    val loading: Boolean = false, val favoritePending: String? = null,
    val refreshing: Boolean = false,
    val error: String? = null, val hasNext: Boolean = false,
)
