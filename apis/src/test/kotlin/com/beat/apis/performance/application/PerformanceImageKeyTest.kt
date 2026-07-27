package com.beat.apis.performance.application

import com.beat.apis.exception.ApiApplicationException
import com.beat.contracts.storage.FileStoragePort
import com.beat.contracts.storage.ImageObjectMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class PerformanceImageKeyTest {

    private val fileStoragePort = mock(FileStoragePort::class.java)

    @Test
    fun `validates an uploaded image in the expected category`() {
        val imageKey = "dev/poster/poster.png"
        `when`(fileStoragePort.findImageObjectMetadata(imageKey))
            .thenReturn(ImageObjectMetadata.of("image/png", 1024L))

        val result = validateStoredPerformanceImage(fileStoragePort, imageKey, "poster")

        assertEquals(imageKey, result)
    }

    @Test
    fun `rejects an uploaded image from another category`() {
        assertThrows(ApiApplicationException::class.java) {
            validateStoredPerformanceImage(fileStoragePort, "dev/staff/staff.png", "cast")
        }
        verifyNoInteractions(fileStoragePort)
    }

    @Test
    fun `rejects an image missing from object storage`() {
        val imageKey = "dev/performance/detail.png"
        `when`(fileStoragePort.findImageObjectMetadata(imageKey)).thenReturn(null)

        assertThrows(ApiApplicationException::class.java) {
            validateStoredPerformanceImage(fileStoragePort, imageKey, "performance")
        }
    }

    @Test
    fun `allows an empty optional image without object storage access`() {
        val result = validateStoredPerformanceImage(
            fileStoragePort = fileStoragePort,
            value = "",
            category = "staff",
            required = false,
        )

        assertEquals("", result)
        verifyNoInteractions(fileStoragePort)
    }
}
