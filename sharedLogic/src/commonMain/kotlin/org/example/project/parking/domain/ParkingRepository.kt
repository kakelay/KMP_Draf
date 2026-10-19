package org.example.project.parking.domain

interface ParkingRepository {
    suspend fun search(search: ParkingSearch): ParkingPage
    suspend fun setFavorite(id: String, favorite: Boolean): ParkingLocation?
}
