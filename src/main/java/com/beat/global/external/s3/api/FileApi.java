package com.beat.global.external.s3.api;

import com.beat.global.common.dto.ErrorResponse;
import com.beat.global.common.dto.SuccessResponse;
import com.beat.global.external.s3.application.dto.PerformanceMakerPresignedUrlFindAllResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Image - Performance PreSigned Url", description = "Performance PreSigned Url 발급 API")
public interface FileApi {

    @Operation(summary = "공연 이미지 업로드 presigned url 발급")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "공연 이미지를 업로드할 PreSigned url이 발행되었습니다.",
                            content = @Content(schema = @Schema(implementation = SuccessResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "S3 PreSigned url을 받아오기에 실패했습니다.",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<SuccessResponse<PerformanceMakerPresignedUrlFindAllResponse>> generateAllPresignedUrls(
            @RequestParam String posterImage,
            @RequestParam(required = false) List<String> castImages,
            @RequestParam(required = false) List<String> staffImages,
            @RequestParam(required = false) List<String> performanceImages
    );
}