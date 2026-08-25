package com.beat.application.frontoffice.booking.booker

import com.beat.application.frontoffice.exception.FrontofficeApplicationException

internal data class GuestBookingIdentity(
    val bookerName: String,
    val phoneNumber: String,
    val birthDate: String,
    val password: String,
)

internal data class BookerContact(
    val name: String,
    val phoneNumber: String,
)

internal fun validateBookerContact(name: String?, phoneNumber: String?): BookerContact {
    if (name == null || phoneNumber == null) {
        throw FrontofficeApplicationException(BookingApplicationErrorCode.REQUIRED_DATA_MISSING)
    }
    if (!NAME_PATTERN.matches(name) || !PHONE_PATTERN.matches(phoneNumber)) {
        throw FrontofficeApplicationException(BookingApplicationErrorCode.INVALID_REQUEST_FORMAT)
    }
    return BookerContact(
        name = name,
        phoneNumber = phoneNumber,
    )
}

internal fun validateGuestBookingIdentity(
    bookerName: String?,
    phoneNumber: String?,
    birthDate: String?,
    password: String?,
): GuestBookingIdentity {
    val contact = validateBookerContact(bookerName, phoneNumber)
    if (birthDate == null || password == null) {
        throw FrontofficeApplicationException(BookingApplicationErrorCode.REQUIRED_DATA_MISSING)
    }
    if (!BIRTH_DATE_PATTERN.matches(birthDate) || !PASSWORD_PATTERN.matches(password)) {
        throw FrontofficeApplicationException(BookingApplicationErrorCode.INVALID_REQUEST_FORMAT)
    }
    return GuestBookingIdentity(
        bookerName = contact.name,
        phoneNumber = contact.phoneNumber,
        birthDate = birthDate,
        password = password,
    )
}

private val NAME_PATTERN = Regex("^[a-zA-Z가-힣]+$")
private val PHONE_PATTERN = Regex("^\\d{3}-\\d{4}-\\d{4}$")
private val BIRTH_DATE_PATTERN = Regex("^\\d{6}$")
private val PASSWORD_PATTERN = Regex("^\\d{4}$")
