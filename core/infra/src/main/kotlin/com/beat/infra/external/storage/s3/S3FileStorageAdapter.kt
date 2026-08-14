package com.beat.infra.external.storage.s3

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.beat.contracts.storage.BannerPresignedUrl
import com.beat.contracts.storage.CarouselPresignedUpload
import com.beat.contracts.storage.CarouselPresignedUrls
import com.beat.contracts.storage.FileStoragePort
import com.beat.contracts.storage.ImageObjectMetadata
import com.beat.contracts.storage.ImagePresignedUpload
import com.beat.contracts.storage.PerformancePresignedUrls
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID

@Service
class S3FileStorageAdapter(
    private val amazonS3: AmazonS3,
) : FileStoragePort {
    @field:Value("\${cloud.s3.bucket}")
    private lateinit var bucket: String

    @field:Value("\${cloud.s3.key-prefix:}")
    private lateinit var keyPrefix: String

    override fun issueAllPresignedUrlsForPerformanceMaker(
        posterImage: String,
        castImages: List<String>,
        staffImages: List<String>,
        performanceImages: List<String>,
    ): PerformancePresignedUrls {
        val uploads = mapOf(
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
            ImagePresignedUpload.of(amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, path)).toString(), path)
        }

    override fun issueAllPresignedUrlsForCarousel(carouselImages: List<String>): CarouselPresignedUrls {
        val uploads = carouselImages.associateWith { image ->
            val path = generatePath("carousel", image)
            CarouselPresignedUpload.of(amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, path)).toString(), path)
        }
        return CarouselPresignedUrls(uploads)
    }

    override fun findImageObjectMetadata(imageKey: String): ImageObjectMetadata? {
        if (!imageKey.startsWith(imageKeyPrefix())) {
            return null
        }
        try {
            val metadata = amazonS3.getObjectMetadata(bucket, imageKey)
            return ImageObjectMetadata.of(metadata.contentType, metadata.contentLength)
        } catch (exception: AmazonS3Exception) {
            if (exception.statusCode == 404) {
                return null
            }
            throw exception
        }
    }

    override fun issuePresignedUrlForBanner(bannerImage: String): BannerPresignedUrl {
        val path = generatePath("banner", bannerImage)
        val url = amazonS3.generatePresignedUrl(buildPresignedUrlRequest(bucket, path))
        return BannerPresignedUrl(url.toString(), path)
    }

    private fun buildPresignedUrlRequest(bucket: String, fileName: String): GeneratePresignedUrlRequest =
        GeneratePresignedUrlRequest(bucket, fileName)
            .withMethod(HttpMethod.PUT)
            .withExpiration(generatePresignedUrlExpiration())

    private fun generatePresignedUrlExpiration(): Date = Date(System.currentTimeMillis() + PRESIGNED_URL_VALIDITY_MILLIS)

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
}
