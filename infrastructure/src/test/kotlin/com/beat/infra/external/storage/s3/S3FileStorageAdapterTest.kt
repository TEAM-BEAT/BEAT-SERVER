package com.beat.infra.external.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils

class S3FileStorageAdapterTest : FunSpec({
    test("Promotion image object가 현재 environment에 존재하면 true를 반환한다") {
        val amazonS3 = mockk<AmazonS3>(relaxed = true)
        val adapter = adapter(amazonS3)
        val metadata = ObjectMetadata().apply {
            contentType = "image/png"
            contentLength = 1024L
        }
        every { amazonS3.getObjectMetadata("bucket", "dev/carousel/image.png") } returns metadata

        adapter.exists("dev/carousel/image.png") shouldBe true
    }

    test("S3가 404를 반환하면 Promotion image가 존재하지 않는 것으로 해석한다") {
        val amazonS3 = mockk<AmazonS3>(relaxed = true)
        val adapter = adapter(amazonS3)
        val exception = AmazonS3Exception("not found").apply { statusCode = 404 }
        every { amazonS3.getObjectMetadata("bucket", "dev/carousel/missing.png") } throws exception

        adapter.exists("dev/carousel/missing.png") shouldBe false
    }

    test("다른 environment key는 S3 조회 없이 거부한다") {
        val amazonS3 = mockk<AmazonS3>(relaxed = true)
        val adapter = adapter(amazonS3)

        adapter.exists("prod/poster/poster.png") shouldBe false
        verify { amazonS3 wasNot Called }
    }
})

private fun adapter(amazonS3: AmazonS3): S3FileStorageAdapter =
    S3FileStorageAdapter(amazonS3).also {
        ReflectionTestUtils.setField(it, "bucket", "bucket")
        ReflectionTestUtils.setField(it, "keyPrefix", "dev")
    }
