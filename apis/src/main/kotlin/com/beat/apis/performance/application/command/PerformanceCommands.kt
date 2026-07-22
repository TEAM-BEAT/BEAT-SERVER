package com.beat.apis.performance.application.command

import java.time.LocalDateTime

data class PerformanceCreateCommand(
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
)

data class PerformanceModifyCommand(
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
)

data class ScheduleCreateCommand(
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
    val scheduleNumber: PerformanceScheduleNumber,
)

data class ScheduleModifyCommand(
    val scheduleId: Long?,
    val performanceDate: LocalDateTime,
    val totalTicketCount: Int,
)

data class CastCreateCommand(val name: String, val role: String, val photo: String)
data class StaffCreateCommand(val name: String, val role: String, val photo: String)
data class PerformanceImageCreateCommand(val image: String)
data class CastModifyCommand(val id: Long?, val name: String, val role: String, val photo: String)
data class StaffModifyCommand(val id: Long?, val name: String, val role: String, val photo: String)
data class PerformanceImageModifyCommand(val id: Long?, val image: String)

enum class PerformanceGenre { BAND, PLAY, DANCE, ETC }

enum class PerformanceBankName {
    NH_NONGHYUP, KAKAOBANK, KB_KOOKMIN, TOSSBANK, SHINHAN, WOORI, IBK_GIUP, HANA,
    SAEMAUL, BUSAN, IMBANK_DAEGU, SINHYEOP, WOOCHAEGUK, SCJEIL, SUHYEOP, NONE,
}

enum class PerformanceScheduleNumber {
    FIRST, SECOND, THIRD, FOURTH, FIFTH, SIXTH, SEVENTH, EIGHTH, NINTH, TENTH,
}
