package com.beat.apps.admin.support

import com.beat.application.admin.promotion.PromotionImageStorage
import com.beat.application.admin.promotion.PromotionImageUpload
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.testcontainers.mysql.MySQLContainer

@TestConfiguration(proxyBeanMethods = false)
class BeatAdminTestContainersConfig {

    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer =
        MySQLContainer("mysql:8.0.39")
            .withDatabaseName("beat_admin_test")
            .withCommand("--default-time-zone=+09:00")

    @Bean
    @Primary
    fun promotionImageStorage(): PromotionImageStorage = object : PromotionImageStorage {
        override fun issueCarouselUploads(imageNames: List<String>): Map<String, PromotionImageUpload> = emptyMap()

        override fun exists(imageKey: String): Boolean = false

        override fun issueBannerUpload(imageName: String): PromotionImageUpload =
            PromotionImageUpload(
                uploadUrl = "https://admin-test.invalid/upload",
                imageKey = "test/banner.png",
            )
    }
}
