package com.beat.application.frontoffice.booking.command

@ConsistentCopyVisibility
data class MemberBookingCommand private constructor(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
) {
    companion object {
        @JvmStatic
        fun of(
            scheduleId: Long,
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
        ): MemberBookingCommand = MemberBookingCommand(
            scheduleId = scheduleId,
            purchaseTicketCount = purchaseTicketCount,
            bookerName = bookerName,
            bookerPhoneNumber = bookerPhoneNumber,
        )
    }
}

@ConsistentCopyVisibility
data class GuestBookingCommand private constructor(
    val scheduleId: Long,
    val purchaseTicketCount: Int,
    val bookerName: String,
    val bookerPhoneNumber: String,
    val birthDate: String,
    val password: String,
) {
    companion object {
        @JvmStatic
        fun of(
            scheduleId: Long,
            purchaseTicketCount: Int,
            bookerName: String,
            bookerPhoneNumber: String,
            birthDate: String,
            password: String,
        ): GuestBookingCommand = GuestBookingCommand(
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
data class GuestBookingAuthenticationCommand private constructor(
    val bookerName: String,
    val birthDate: String,
    val bookerPhoneNumber: String,
    val password: String,
) {
    companion object {
        @JvmStatic
        fun of(
            bookerName: String,
            birthDate: String,
            bookerPhoneNumber: String,
            password: String,
        ): GuestBookingAuthenticationCommand = GuestBookingAuthenticationCommand(
            bookerName = bookerName,
            birthDate = birthDate,
            bookerPhoneNumber = bookerPhoneNumber,
            password = password,
        )
    }
}

@ConsistentCopyVisibility
data class BookingRefundCommand private constructor(
    val bookingId: Long,
    val bankName: String?,
    val accountNumber: String?,
    val accountHolder: String?,
) {
    companion object {
        @JvmStatic
        fun of(
            bookingId: Long,
            bankName: String?,
            accountNumber: String?,
            accountHolder: String?,
        ): BookingRefundCommand = BookingRefundCommand(
            bookingId = bookingId,
            bankName = bankName,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
        )
    }
}

@ConsistentCopyVisibility
data class BookingCancelCommand private constructor(
    val bookingId: Long,
) {
    companion object {
        @JvmStatic
        fun from(bookingId: Long): BookingCancelCommand = BookingCancelCommand(bookingId)
    }
}
