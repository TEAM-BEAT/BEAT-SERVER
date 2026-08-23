package com.beat.apis.file.api

import com.beat.apis.file.api.response.FileSuccessCode
import com.beat.apis.file.api.response.PerformanceMakerPresignedUrlFindAllResponse
import com.beat.apis.file.facade.FileFacade
import com.beat.apis.response.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/files")
class FileController(
    private val fileFacade: FileFacade,
) : FileApi {

    @GetMapping("/presigned-url")
    override fun generateAllPresignedUrls(
        @RequestParam posterImage: String,
        @RequestParam(required = false) castImages: List<String>?,
        @RequestParam(required = false) staffImages: List<String>?,
        @RequestParam(required = false) performanceImages: List<String>?,
    ): ResponseEntity<SuccessResponse<PerformanceMakerPresignedUrlFindAllResponse>> {
        val response = fileFacade.issueAllPresignedUrlsForPerformanceMaker(
            posterImage,
            castImages,
            staffImages,
            performanceImages,
        )
        return ResponseEntity.ok(SuccessResponse.of(FileSuccessCode.PERFORMANCE_MAKER_PRESIGNED_URL_ISSUED, response))
    }
}
