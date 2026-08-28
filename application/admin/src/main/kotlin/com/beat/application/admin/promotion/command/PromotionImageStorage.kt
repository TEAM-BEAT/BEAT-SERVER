package com.beat.application.admin.promotion.command

data class PromotionImageUpload(
    val uploadUrl: String,
    val imageKey: String,
)

interface PromotionImageStorage {
    fun issueCarouselUploads(imageNames: List<String>): Map<String, PromotionImageUpload>

    fun issueBannerUpload(imageName: String): PromotionImageUpload

    fun exists(imageKey: String): Boolean
}
