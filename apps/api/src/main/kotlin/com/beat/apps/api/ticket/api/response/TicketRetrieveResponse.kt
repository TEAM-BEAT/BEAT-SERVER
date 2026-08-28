package com.beat.apps.api.ticket.api.response

import com.beat.application.frontoffice.ticket.maker.query.TicketRetrieveResult
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연의 예매자 목록과 공연별 티켓 집계 정보입니다.")
@ConsistentCopyVisibility
data class TicketRetrieveResponse
private constructor(
    @field:Schema(
        description = "공연 제목입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "title",
    )
    val performanceTitle: String,
    @field:Schema(
        description = "공연 팀 이름입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "team",
    )
    val performanceTeamName: String,
    @field:Schema(
        description = "공연의 전체 회차 수입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "1",
    )
    val totalScheduleCount: Int,
    @field:Schema(
        description = "공연의 전체 판매 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "100",
    )
    val totalPerformanceTicketCount: Int,
    @field:Schema(
        description = "공연에서 판매된 티켓 수량입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "10",
    )
    val totalPerformanceSoldTicketCount: Int,
    @field:Schema(
        description = "조건에 맞는 예매자 상세 목록입니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "[]",
    )
    val bookingList: List<TicketDetail>,
) {
    companion object {
        fun from(result: TicketRetrieveResult): TicketRetrieveResponse =
            TicketRetrieveResponse(
                performanceTitle = result.performanceTitle,
                performanceTeamName = result.performanceTeamName,
                totalScheduleCount = result.totalScheduleCount,
                totalPerformanceTicketCount = result.totalPerformanceTicketCount,
                totalPerformanceSoldTicketCount = result.totalPerformanceSoldTicketCount,
                bookingList = result.bookingList.map(TicketDetail::from),
            )
    }
}
