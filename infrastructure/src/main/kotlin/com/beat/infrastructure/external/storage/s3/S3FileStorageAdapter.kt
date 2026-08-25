package com.beat.infrastructure.external.storage.s3

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.beat.application.admin.promotion.PromotionImageStorage
import com.beat.application.admin.promotion.PromotionImageUpload
import com.beat.application.frontoffice.performance.maker.command.ImagePresignedUpload
import com.beat.application.frontoffice.performance.maker.command.PerformanceImageStorage
import com.beat.application.frontoffice.performance.maker.command.PerformancePresignedUrls
import java.util.Date
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
internal class S3FileStorageAdapter(private val amazonS3: AmazonS3) :
    PerformanceImageStorage, PromotionImageStorage {
    @field:Value("\${cloud.s3.bucket}") private lateinit var bucket: String

    @field:Value("\${cloud.s3.key-prefix:}") private lateinit var keyPrefix: String

    override fun issueAllPresignedUrls(
        posterImage: String,
        castImages: List<String>,
        staffImages: List<String>,
        performanceImages: List<String>,
    ): PerformancePresignedUrls {
        val uploads =
            mapOf(
                "poster" to listOf(posterImage).toPresignedUploads("poster"),
                "cast" to castImages.toPresignedUploads("cast"),
                "staff" to staffImages.toPresignedUploads("staff"),
                "performance" to performanceImages.toPresignedUploads("performance"),
            )
        return PerformancePresignedUrls(uploads)
    }

    private fun List<String>.toPresignedUploads(prefix: String): Map<String, ImagePresignedUpload> =
        associateWith { image ->
            val path = generatePath(prefix, image)
            ImagePresignedUpload.of(
                amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, path)).toString(),
                path,
            )
        }

    override fun issueCarouselUploads(imageNames: List<String>): Map<String, PromotionImageUpload> =
        imageNames.associateWith { image ->
            val path = generatePath("carousel", image)
            PromotionImageUpload(
                amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, path)).toString(),
                path,
            )
        }

    private fun findImageObjectMetadata(imageKey: String): ObjectMetadata? {
        if (!imageKey.startsWith(imageKeyPrefix())) {
            return null
        }
        try {
            val metadata = amazonS3.getObjectMetadata(bucket, imageKey)
            return ObjectMetadata(metadata.contentType, metadata.contentLength)
        } catch (exception: AmazonS3Exception) {
            if (exception.statusCode == 404) {
                return null
            }
            throw exception
        }
    }

    override fun exists(imageKey: String): Boolean = findImageObjectMetadata(imageKey) != null

    override fun issueBannerUpload(imageName: String): PromotionImageUpload {
        val path = generatePath("banner", imageName)
        val url = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, path))
        return PromotionImageUpload(url.toString(), path)
    }

    private fun buildPresignedUrlRequest(
        bucket: String,
        fileName: String,
    ): GeneratePresignedUrlRequest =
        GeneratePresignedUrlRequest(bucket, fileName)
            .withMethod(HttpMethod.PUT)
            .withExpiration(generatePresignedUrlExpiration())

    private fun generatePresignedUrlExpiration(): Date =
        Date(System.currentTimeMillis() + PRESIGNED_URL_VALIDITY_MILLIS)

    private fun generatePath(prefix: String, fileName: String): String {
        val filePath = "$prefix/${UUID.randomUUID()}-$fileName"
        val normalizedKeyPrefix = normalizeKeyPrefix()
        return if (normalizedKeyPrefix.isEmpty()) filePath else "$normalizedKeyPrefix/$filePath"
    }

    private fun normalizeKeyPrefix(): String {
        if (!::keyPrefix.isInitialized || keyPrefix.isBlank()) {
            return ""
        }
        return keyPrefix.replace(LEADING_SLASHES, "").replace(TRAILING_SLASHES, "")
    }

    private fun imageKeyPrefix(): String {
        val normalizedKeyPrefix = normalizeKeyPrefix()
        return if (normalizedKeyPrefix.isEmpty()) "" else "$normalizedKeyPrefix/"
    }

    private companion object {
        const val PRESIGNED_URL_VALIDITY_MILLIS = 15L * 60 * 1000
        val LEADING_SLASHES = Regex("^/+")
        val TRAILING_SLASHES = Regex("/+$")
    }

    private data class ObjectMetadata(
        val contentType: String?,
        val contentLength: Long,
    )
}
