package org.example.project.parking.domain

data class ParkingLocation(
    val id: String, val name: String, val address: String,
    val latitude: Double, val longitude: Double,
    val pricePerHour: String, val availableSpaces: Int, val isFavorite: Boolean,
)

data class ParkingPage(val locations: List<ParkingLocation>, val count: Long, val page: Int, val size: Int) {
    val hasNext: Boolean get() = (page.toLong() + 1) * size < count
}

data class ParkingSearch(
    val query: String = "", val category: String? = null, val page: Int = 0, val size: Int = 20,
    val latitude: Double? = null, val longitude: Double? = null,
) {
    init {
        require(query.length <= 160 && page >= 0 && size in 1..100)
        require(category == null || category in setOf("all", "mall", "office", "hotel", "restaurant", "hospital", "school", "event", "private"))
        require(latitude == null || latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude == null || longitude.isFinite() && longitude in -180.0..180.0)
    }
}
