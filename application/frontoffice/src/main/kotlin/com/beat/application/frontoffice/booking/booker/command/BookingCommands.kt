package com.beat.application.frontoffice.booking.booker.command

@ConsistentCopyVisibility
data class MemberBookingCommand
private constructor(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
) {
    companion object {
        fun of(
            scheduleId: Long,
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
        ): MemberBookingCommand =
            MemberBookingCommand(
                scheduleId = scheduleId,
                purchaseTicketCount = purchaseTicketCount,
                bookerName = bookerName,
                bookerPhoneNumber = bookerPhoneNumber,
            )
    }
}

@ConsistentCopyVisibility
data class GuestBookingCommand
private constructor(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val birthDate: String,
    val password: String,
) {
    companion object {
        fun of(
            scheduleId: Long,
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
            birthDate: String,
            password: String,
        ): GuestBookingCommand =
            GuestBookingCommand(
                scheduleId = scheduleId,
                purchaseTicketCount = purchaseTicketCount,
                bookerName = bookerName,
                bookerPhoneNumber = bookerPhoneNumber,
                birthDate = birthDate,
                password = password,
            )
    }
}

@ConsistentCopyVisibility
data class GuestBookingAuthenticationCommand
private constructor(
    val bookerName: String,
    val birthDate: String,
    val bookerPhoneNumber: String,
    val password: String,
) {
    companion object {
        fun of(
            bookerName: String,
            birthDate: String,
            bookerPhoneNumber: String,
            password: String,
        ): GuestBookingAuthenticationCommand =
            GuestBookingAuthenticationCommand(
                bookerName = bookerName,
                birthDate = birthDate,
                bookerPhoneNumber = bookerPhoneNumber,
                password = password,
            )
    }
}

data class BookingActorCommand(
    val memberId: Long?,
    val guestSessionToken: String?,
)

@ConsistentCopyVisibility
data class BookingRefundCommand
private constructor(
    val bookingId: Long,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
) {
    companion object {
        fun of(
            bookingId: Long,
            bankName: String?,
            accountNumber: String?,
            accountHolder: String?,
        ): BookingRefundCommand =
            BookingRefundCommand(
                bookingId = bookingId,
                bankName = bankName,
                accountNumber = accountNumber,
                accountHolder = accountHolder,
            )
    }
}

@ConsistentCopyVisibility
data class BookingCancelCommand private constructor(val bookingId: Long) {
    companion object {
        fun from(bookingId: Long): BookingCancelCommand = BookingCancelCommand(bookingId)
    }
}
