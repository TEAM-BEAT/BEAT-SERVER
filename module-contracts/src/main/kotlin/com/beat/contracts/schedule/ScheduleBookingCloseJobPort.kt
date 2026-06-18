package com.beat.contracts.schedule


interface ScheduleBookingCloseJobPort {

    fun registerOrRefresh(target: ScheduleBookingCloseJobTarget)

    fun cancel(target: ScheduleBookingCloseJobTarget)
}
