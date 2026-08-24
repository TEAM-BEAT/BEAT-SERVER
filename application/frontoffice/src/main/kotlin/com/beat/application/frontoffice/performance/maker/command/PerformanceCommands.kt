package com.beat.application.frontoffice.performance.maker.command

import java.time.LocalDateTime

@ConsistentCopyVisibility
data class PerformanceCreateCommand private constructor(
    val performanceTitle: String,
    val genre: PerformanceGenre,
    val runningTime: Int,
    val performanceDescription: String,
    val performanceAttentionNote: String,
    val bankName: PerformanceBankName?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val roadAddressName: String,
    val placeDetailAddress: String,
    val latitude: String,
    val longitude: String,
    val performanceContact: String,
    val ticketPrice: Int,
    val schedules: List<ScheduleCreateCommand>,
    val casts: List<CastCreateCommand>,
    val staffs: List<StaffCreateCommand>,
    val images: List<PerformanceImageCreateCommand>,
) {
    companion object {
        fun of(
            performanceTitle: String,
            genre: PerformanceGenre,
            runningTime: Int,
            performanceDescription: String,
            performanceAttentionNote: String,
            bankName: PerformanceBankName?,
            accountNumber: String?,
            accountHolder: String?,
            posterImage: String,
            performanceTeamName: String,
            performanceVenue: String,
            roadAddressName: String,
            placeDetailAddress: String,
            latitude: String,
            longitude: String,
            performanceContact: String,
            ticketPrice: Int,
            schedules: List<ScheduleCreateCommand>,
            casts: List<CastCreateCommand>,
            staffs: List<StaffCreateCommand>,
            images: List<PerformanceImageCreateCommand>,
        ): PerformanceCreateCommand = PerformanceCreateCommand(
            performanceTitle = performanceTitle,
            genre = genre,
            runningTime = runningTime,
            performanceDescription = performanceDescription,
            performanceAttentionNote = performanceAttentionNote,
            bankName = bankName,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
            posterImage = posterImage,
            performanceTeamName = performanceTeamName,
            performanceVenue = performanceVenue,
            roadAddressName = roadAddressName,
            placeDetailAddress = placeDetailAddress,
            latitude = latitude,
            longitude = longitude,
            performanceContact = performanceContact,
            ticketPrice = ticketPrice,
            schedules = schedules,
            casts = casts,
            staffs = staffs,
            images = images,
        )
    }
}

@ConsistentCopyVisibility
data class PerformanceModifyCommand private constructor(
    val performanceId: Long,
    val performanceTitle: String,
    val genre: PerformanceGenre,
    val runningTime: Int,
    val performanceDescription: String,
    val performanceAttentionNote: String,
    val bankName: PerformanceBankName?,
    val accountNumber: String?,
    val accountHolder: String?,
    val posterImage: String,
    val performanceTeamName: String,
    val performanceVenue: String,
    val roadAddressName: String,
    val placeDetailAddress: String,
    val latitude: String,
    val longitude: String,
    val performanceContact: String,
    val ticketPrice: Int,
    val schedules: List<ScheduleModifyCommand>,
    val casts: List<CastModifyCommand>,
    val staffs: List<StaffModifyCommand>,
    val images: List<PerformanceImageModifyCommand>,
) {
    companion object {
        fun of(
            performanceId: Long,
            performanceTitle: String,
            genre: PerformanceGenre,
            runningTime: Int,
            performanceDescription: String,
            performanceAttentionNote: String,
            bankName: PerformanceBankName?,
            accountNumber: String?,
            accountHolder: String?,
            posterImage: String,
            performanceTeamName: String,
            performanceVenue: String,
            roadAddressName: String,
            placeDetailAddress: String,
            latitude: String,
            longitude: String,
            performanceContact: String,
            ticketPrice: Int,
            schedules: List<ScheduleModifyCommand>,
            casts: List<CastModifyCommand>,
            staffs: List<StaffModifyCommand>,
            images: List<PerformanceImageModifyCommand>,
        ): PerformanceModifyCommand = PerformanceModifyCommand(
            performanceId = performanceId,
            performanceTitle = performanceTitle,
            genre = genre,
            runningTime = runningTime,
            performanceDescription = performanceDescription,
            performanceAttentionNote = performanceAttentionNote,
            bankName = bankName,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
            posterImage = posterImage,
            performanceTeamName = performanceTeamName,
            performanceVenue = performanceVenue,
            roadAddressName = roadAddressName,
            placeDetailAddress = placeDetailAddress,
            latitude = latitude,
            longitude = longitude,
            performanceContact = performanceContact,
            ticketPrice = ticketPrice,
            schedules = schedules,
            casts = casts,
            staffs = staffs,
            images = images,
        )
    }
}

@ConsistentCopyVisibility
data class ScheduleCreateCommand private constructor(
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
    val scheduleNumber: PerformanceScheduleNumber,
) {
    companion object {
        fun of(
            performanceDate: LocalDateTime,
            totalTicketCount: Int,
            scheduleNumber: PerformanceScheduleNumber,
        ): ScheduleCreateCommand = ScheduleCreateCommand(performanceDate, totalTicketCount, scheduleNumber)
    }
}

@ConsistentCopyVisibility
data class ScheduleModifyCommand private constructor(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
) {
    companion object {
        fun of(scheduleId: Long?, performanceDate: LocalDateTime, totalTicketCount: Int): ScheduleModifyCommand =
            ScheduleModifyCommand(scheduleId, performanceDate, totalTicketCount)
    }
}

@ConsistentCopyVisibility
data class CastCreateCommand private constructor(val name: String, val role: String, val photo: String) {
    companion object {
        fun of(name: String, role: String, photo: String): CastCreateCommand = CastCreateCommand(name, role, photo)
    }
}

@ConsistentCopyVisibility
data class StaffCreateCommand private constructor(val name: String, val role: String, val photo: String) {
    companion object {
        fun of(name: String, role: String, photo: String): StaffCreateCommand = StaffCreateCommand(name, role, photo)
    }
}

@ConsistentCopyVisibility
data class PerformanceImageCreateCommand private constructor(val image: String) {
    companion object {
        fun from(image: String): PerformanceImageCreateCommand = PerformanceImageCreateCommand(image)
    }
}

@ConsistentCopyVisibility
data class CastModifyCommand private constructor(val id: Long?, val name: String, val role: String, val photo: String) {
    companion object {
        fun of(id: Long?, name: String, role: String, photo: String): CastModifyCommand =
            CastModifyCommand(id, name, role, photo)
    }
}

@ConsistentCopyVisibility
data class StaffModifyCommand private constructor(val id: Long?, val name: String, val role: String, val photo: String) {
    companion object {
        fun of(id: Long?, name: String, role: String, photo: String): StaffModifyCommand =
            StaffModifyCommand(id, name, role, photo)
    }
}

@ConsistentCopyVisibility
data class PerformanceImageModifyCommand private constructor(val id: Long?, val image: String) {
    companion object {
        fun of(id: Long?, image: String): PerformanceImageModifyCommand = PerformanceImageModifyCommand(id, image)
    }
}

enum class PerformanceGenre { BAND, PLAY, DANCE, ETC }

enum class PerformanceBankName {
    NH_NONGHYUP, KAKAOBANK, KB_KOOKMIN, TOSSBANK, SHINHAN, WOORI, IBK_GIUP, HANA,
    SAEMAUL, BUSAN, IMBANK_DAEGU, SINHYEOP, WOOCHAEGUK, SCJEIL, SUHYEOP, NONE,
}

enum class PerformanceScheduleNumber {
    FIRST, SECOND, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH, EIGHTH, NINTH, TENTH,
}
