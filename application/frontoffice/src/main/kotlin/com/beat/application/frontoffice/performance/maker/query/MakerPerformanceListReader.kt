package com.beat.application.frontoffice.performance.maker.query

fun interface MakerPerformanceListReader {
    fun findByUserId(userId: Long): List<MakerPerformanceListItemReadModel>
}
