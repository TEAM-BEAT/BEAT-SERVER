package com.beat.domain.staff.repository

import com.beat.domain.staff.domain.Staff
import java.util.*

@JvmSuppressWildcards
interface StaffRepository {
    fun findById(staffId: Long?): Optional<Staff>

    fun findAllByPerformanceId(performanceId: Long?): List<Staff>

    fun findIdsByPerformanceId(performanceId: Long?): List<Long>

    fun save(staff: Staff): Staff

    fun saveAll(staffs: List<Staff>): List<Staff>

    fun delete(staff: Staff)

    fun deleteByPerformanceId(performanceId: Long?)
}
