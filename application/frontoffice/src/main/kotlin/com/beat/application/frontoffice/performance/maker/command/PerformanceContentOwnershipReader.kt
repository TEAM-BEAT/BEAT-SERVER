package com.beat.application.frontoffice.performance.maker.command

interface PerformanceContentOwnershipReader {
    fun findPerformanceIdByCastId(castId: Long): Long?

    fun findPerformanceIdByStaffId(staffId: Long): Long?

    fun findPerformanceIdByImageId(imageId: Long): Long?
}
