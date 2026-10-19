package org.example.project.parking.domain

/** Failures presentation can understand without depending on HTTP or Firebase SDKs. */
enum class ParkingFailureReason { SIGN_IN_REQUIRED, SESSION_EXPIRED, ACCESS_DENIED, REQUEST_FAILED, CONNECTION_FAILED }

class ParkingException(
    val reason: ParkingFailureReason,
    val referenceNumber: String? = null,
    cause: Throwable? = null,
) : Exception(reason.name, cause)
