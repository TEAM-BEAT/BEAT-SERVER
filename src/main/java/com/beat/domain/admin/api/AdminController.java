package com.beat.domain.admin.api;

import com.beat.domain.admin.application.AdminUserManagementService;
import com.beat.domain.admin.exception.AdminSuccessCode;
import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.global.auth.annotation.CurrentMember;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.external.s3.application.FileService;
import com.beat.global.external.s3.application.dto.BannerPresignedUrlFindResponse;
import com.beat.global.external.s3.application.dto.CarouselPresignedUrlFindAllResponse;
import com.beat.global.external.s3.exception.FileSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController implements AdminApi {

    private final AdminUserManagementService adminUserManagementService;
    private final FileService fileService;

    @Override
    @GetMapping("/users")
    public ResponseEntity<SuccessResponse<UserFindAllResponse>> readAllUsers(
            @CurrentMember Long memberId) {
        UserFindAllResponse response = adminUserManagementService.findAllUsers(memberId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(AdminSuccessCode.FETCH_ALL_USERS_SUCCESS, response));
    }

    @GetMapping("/carousel/presigned-url")
    @Override
    public ResponseEntity<SuccessResponse<CarouselPresignedUrlFindAllResponse>> createAllCarouselPresignedUrls(
            @CurrentMember Long memberId,
            @RequestParam List<String> carouselImages) {
        CarouselPresignedUrlFindAllResponse response = fileService.issueAllPresignedUrlsForCarousel(memberId, carouselImages);
        return ResponseEntity.ok(SuccessResponse.of(AdminSuccessCode.CAROUSEL_PRESIGNED_URL_ISSUED, response));
    }

    @GetMapping("/banner/presigned-url")
    @Override
    public ResponseEntity<SuccessResponse<BannerPresignedUrlFindResponse>> createBannerPresignedUrl(
            @CurrentMember Long memberId,
            @RequestParam String bannerImage) {
        BannerPresignedUrlFindResponse response = fileService.issuePresignedUrlForBanner(memberId, bannerImage);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.of(AdminSuccessCode.BANNER_PRESIGNED_URL_ISSUED, response));
    }
}