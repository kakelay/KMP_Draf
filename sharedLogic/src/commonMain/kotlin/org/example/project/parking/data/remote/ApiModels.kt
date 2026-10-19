package org.example.project.parking.data.remote

import org.example.project.parking.domain.ParkingLocation
import org.example.project.parking.domain.ParkingPage

import kotlinx.serialization.Serializable

@Serializable
internal data class ApiEnvelope<T>(
    val responseCode: String,
    val message: String,
    val referenceNumber: String? = null,
    val data: T? = null,
)

@Serializable
data class ParkingCategory(val id: String, val label: String)

// Decimal prices remain strings at the domain boundary; never use Double for money.
@Serializable
internal data class ParkingDto(
    val id: String, val name: String, val address: String,
    val latitude: Double, val longitude: Double,
    val pricePerHour: kotlinx.serialization.json.JsonPrimitive,
    val availableSpaces: Int, val isFavorite: Boolean,
)



internal fun ParkingDto.toDomain() = ParkingLocation(
    id, name, address, latitude, longitude, pricePerHour.content, availableSpaces, isFavorite,
)

@Serializable
internal data class ParkingPageDto(val data: List<ParkingDto>, val count: Long, val page: Int, val size: Int)


internal fun ParkingPageDto.toDomain() = ParkingPage(data.map { it.toDomain() }, count, page, size)

@Serializable
data class Account(val username: String)

@Serializable
data class SupportLinks(val about: String?, val contact: String?, val policy: String?, val terms: String?)

@Serializable
data class NotificationTopic(val id: String, val topic: String)

@Serializable
data class AppHome(
    val hasZone: Boolean, val canSwitchZone: Boolean, val hasUnreadNotifications: Boolean,
    val requiredUpdate: Boolean, val availableUpdate: Boolean,
    val topics: List<NotificationTopic>, val supportLinks: SupportLinks,
)

@Serializable
data class CustomerZone(
    val id: String, val name: String, val openingTime: String?, val closingTime: String?,
    val location: String?, val isSelected: Boolean,
)

@Serializable
data class CustomerNotification(
    val id: String, val title: String, val body: String, val createdAt: String,
    val readAt: String? = null, val isRead: Boolean,
)

@Serializable
data class NotificationPage(val data: List<CustomerNotification>, val count: Long, val skip: Int, val take: Int)
