package com.beat.infra.persistence.performance.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDate

@Embeddable
internal class PerformancePeriodJpaValue(
    startDate: LocalDate?,
    endDate: LocalDate?,
) {
    @Column(name = "performance_start_date")
    var startDate: LocalDate? = startDate
        protected set

    @Column(name = "performance_end_date")
    var endDate: LocalDate? = endDate
        protected set
}
