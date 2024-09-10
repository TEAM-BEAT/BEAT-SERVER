package com.beat.global.external.s3.api;

import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.external.s3.exception.FileSuccessCode;
import com.beat.global.external.s3.application.FileService;
import com.beat.global.external.s3.application.dto.PerformanceMakerPresignedUrlFindAllResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController implements FileApi {

    private final FileService fileService;

    @GetMapping("/presigned-url")
    @Override
    public ResponseEntity<SuccessResponse<PerformanceMakerPresignedUrlFindAllResponse>> generateAllPresignedUrls(
            @RequestParam String posterImage,
            @RequestParam(required = false) List<String> castImages,
            @RequestParam(required = false) List<String> staffImages,
            @RequestParam(required = false) List<String> performanceImages) {
        // 토큰 주도록 변경이 필요
        if (castImages == null) {
            castImages = List.of();
        }
        if (staffImages == null) {
            staffImages = List.of();
        }
        if (performanceImages == null) {
            performanceImages = List.of();
        }

        PerformanceMakerPresignedUrlFindAllResponse response = fileService.issueAllPresignedUrlsForPerformanceMaker(posterImage, castImages, staffImages, performanceImages);
        return ResponseEntity.ok(SuccessResponse.of(FileSuccessCode.PERFORMANCE_MAKER_PRESIGNED_URL_ISSUED, response));
    }
}