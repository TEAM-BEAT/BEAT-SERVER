package com.beat.domain.booking.api;

import com.beat.domain.booking.application.TicketService;
import com.beat.domain.booking.application.dto.TicketDeleteRequest;
import com.beat.domain.booking.application.dto.TicketRetrieveResponse;
import com.beat.domain.booking.application.dto.TicketUpdateRequest;
import com.beat.domain.booking.exception.BookingSuccessCode;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.domain.schedule.domain.ScheduleNumber;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @Operation(summary = "예매자 목록 조회 API", description = "메이커가 자신의 공연에 대한 예매자 목록을 조회하는 GET API입니다.")
    @GetMapping("/{performanceId}")
    public ResponseEntity<SuccessResponse<TicketRetrieveResponse>> getTickets(
            @CurrentMember Long memberId,
            @PathVariable Long performanceId,
            @RequestParam(required = false) ScheduleNumber scheduleNumber,
            @RequestParam(required = false) Boolean isPaymentCompleted) {
        TicketRetrieveResponse response = ticketService.getTickets(memberId, performanceId, scheduleNumber, isPaymentCompleted);
        return ResponseEntity.ok(SuccessResponse.of(BookingSuccessCode.TICKET_RETRIEVE_SUCCESS, response));
    }

    @Operation(summary = "예매자 입금여부 수정 및 웹발신 API", description = "메이커가 자신의 공연에 대한 예매자의 입금여부 정보를 수정한 뒤 예매확정 웹발신을 보내는 PUT API입니다.")
    @PutMapping("/{performanceId}") // 이 부분 body로 performanceId를 넘기기에 수정 필요!!
    public ResponseEntity<SuccessResponse<Void>> updateTickets(
            @CurrentMember Long memberId,
            @PathVariable Long performanceId,
            @RequestBody TicketUpdateRequest request) {
        ticketService.updateTickets(memberId, request);
        return ResponseEntity.ok(SuccessResponse.of(BookingSuccessCode.TICKET_UPDATE_SUCCESS, null));
    }

    @Operation(summary = "예매자 삭제 API", description = "메이커가 자신의 공연에 대한 예매자의 정보를 삭제하는 DELETE API입니다.")
    @DeleteMapping
    public ResponseEntity<SuccessResponse<Void>> deleteTickets(
            @CurrentMember Long memberId,
            @RequestBody TicketDeleteRequest ticketDeleteRequest) {
        ticketService.deleteTickets(memberId, ticketDeleteRequest);
        return ResponseEntity.ok(SuccessResponse.from(BookingSuccessCode.TICKET_DELETE_SUCCESS));
    }
}
