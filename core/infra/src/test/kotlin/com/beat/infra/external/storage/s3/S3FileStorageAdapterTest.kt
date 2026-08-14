package com.beat.infra.external.storage.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ObjectMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
class S3FileStorageAdapterTest {
    @Mock
    private lateinit var amazonS3: AmazonS3

    @Test
    fun findImageObjectMetadataReturnsHeadObjectMetadata() {
        val adapter = adapter()
        val metadata = ObjectMetadata().apply {
            contentType = "image/png"
            contentLength = 1024L
        }
        Mockito.`when`(amazonS3.getObjectMetadata("bucket", "dev/carousel/image.png")).thenReturn(metadata)

        val result = adapter.findImageObjectMetadata("dev/carousel/image.png")

        assertEquals("image/png", result?.contentType)
        assertEquals(1024L, result?.contentLength)
    }

    @Test
    fun findImageObjectMetadataReturnsNullWhenObjectDoesNotExist() {
        val adapter = adapter()
        val exception = AmazonS3Exception("not found").apply { statusCode = 404 }
        Mockito.`when`(amazonS3.getObjectMetadata("bucket", "dev/carousel/missing.png")).thenThrow(exception)

        assertNull(adapter.findImageObjectMetadata("dev/carousel/missing.png"))
    }

    @Test
    fun findImageObjectMetadataRejectsOtherEnvironmentsWithoutHeadObject() {
        val adapter = adapter()

        assertNull(adapter.findImageObjectMetadata("prod/poster/poster.png"))
        Mockito.verifyNoInteractions(amazonS3)
    }

    private fun adapter(): S3FileStorageAdapter =
        S3FileStorageAdapter(amazonS3).also {
            ReflectionTestUtils.setField(it, "bucket", "bucket")
            ReflectionTestUtils.setField(it, "keyPrefix", "dev")
        }
}
