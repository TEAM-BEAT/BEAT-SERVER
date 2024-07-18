package com.beat.global.external.s3.controller;

import com.beat.global.external.s3.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "presigned-url API", description = "S3에 업로드 할 수 있는 유효한 url을 주는 GET API입니다.")
    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, Map<String, String>>> getPresignedUrls(
            @RequestParam String posterImage,
            @RequestParam(required = false) List<String> castImages,
            @RequestParam(required = false) List<String> staffImages) {
        if (castImages == null) {
            castImages = List.of();
        }
        if (staffImages == null) {
            staffImages = List.of();
        }
        Map<String, Map<String, String>> response = fileService.getPresignedUrls(posterImage, castImages, staffImages);
        return ResponseEntity.ok(response);
    }
}