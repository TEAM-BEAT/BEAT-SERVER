package com.beat.batch.booking.job

import com.beat.application.system.booking.command.TicketCleanupService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.scheduling.annotation.Scheduled

class TicketCleanupJobSpec : FunSpec({
    test("04시 cleanup cron과 maintenance scheduler 계약을 유지한다") {
        val scheduled = TicketCleanupJob::class.java
            .getDeclaredMethod("deleteOldCancelledBookings")
            .getAnnotation(Scheduled::class.java)

        scheduled.cron shouldBe "0 0 4 * * ?"
        scheduled.scheduler shouldBe "maintenanceTaskScheduler"
    }

    test("현재 runtime이 scheduler owner이면 System use case를 호출한다") {
        val service = mock(TicketCleanupService::class.java)

        TicketCleanupJob(service, true).deleteOldCancelledBookings()

        verify(service).deleteOldCancelledBookings()
    }

    test("현재 runtime이 scheduler owner가 아니면 실행하지 않는다") {
        val service = mock(TicketCleanupService::class.java)

        TicketCleanupJob(service, false).deleteOldCancelledBookings()

        verifyNoInteractions(service)
    }
})
