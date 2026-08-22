package com.beat.apis.schedule.api.response

import com.beat.apis.response.SuccessCode

enum class ScheduleSuccessCode(
    override val status: Int,
    override val message: String,
) : SuccessCode {
    TICKET_AVAILABILITY_RETRIEVAL_SUCCESS(200, "티켓 수량 조회가 성공적으로 완료되었습니다.")
    ;

}
