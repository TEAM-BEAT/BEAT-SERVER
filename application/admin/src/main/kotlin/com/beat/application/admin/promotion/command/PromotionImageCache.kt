package com.beat.application.admin.promotion.command

fun interface PromotionImageCache {
    fun preWarm(imageKey: String)
}
