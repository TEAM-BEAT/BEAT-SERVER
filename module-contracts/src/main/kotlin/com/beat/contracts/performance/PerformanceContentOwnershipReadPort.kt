package com.beat.contracts.performance

interface PerformanceContentOwnershipReadPort {
    fun findPerformanceIdByCastId(castId: Long): Long?

    fun findPerformanceIdByStaffId(staffId: Long): Long?

    fun findPerformanceIdByImageId(imageId: Long): Long?
}
