package com.beat.application.frontoffice.booking.booker.experiment

interface StockContentionScheduleStore {
    fun findBookingMetadataById(scheduleId: Long): ScheduleBookingMetadata?

    fun find(
        scheduleId: Long,
        forUpdate: Boolean,
        withVersion: Boolean,
    ): ScheduleStockState?

    fun reserveWithPessimisticLock(scheduleId: Long, ticketCount: Int): Int

    fun reserveWithOptimisticCas(
        scheduleId: Long,
        ticketCount: Int,
        version: Long,
    ): Int

    fun reserveWithAtomicUpdate(scheduleId: Long, ticketCount: Int): Int

    fun reserveWithRedisLock(scheduleId: Long, ticketCount: Int): Int
}
