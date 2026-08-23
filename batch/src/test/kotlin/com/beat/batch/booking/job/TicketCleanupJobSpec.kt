package com.beat.batch.booking.job

import com.beat.application.system.booking.command.TicketCleanupService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.springframework.scheduling.annotation.Scheduled

class TicketCleanupJobSpec : FunSpec({
    test("04시 cleanup cron과 maintenance scheduler 계약을 유지한다") {
        val scheduled = TicketCleanupJob::class.java
            .getDeclaredMethod("deleteOldCancelledBookings")
            .getAnnotation(Scheduled::class.java)

        scheduled.cron shouldBe "0 0 4 * * ?"
        scheduled.scheduler shouldBe "maintenanceTaskScheduler"
    }

    test("deleteOldCancelledBookings는 System use case를 위임 호출한다") {
        val service = mockk<TicketCleanupService>(relaxed = true)

        TicketCleanupJob(service).deleteOldCancelledBookings()

        verify { service.deleteOldCancelledBookings() }
    }
})
