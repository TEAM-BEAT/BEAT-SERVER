package com.beat.domain.performance.api;

import com.beat.domain.performance.application.PerformanceService;
import com.beat.domain.performance.application.dto.home.HomeRequest;
import com.beat.domain.performance.application.dto.home.HomeResponse;
import com.beat.domain.performance.domain.Genre;
import com.beat.domain.performance.exception.PerformanceSuccessCode;
import com.beat.global.common.dto.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class HomeController {

    private final PerformanceService performanceService;

    @Operation(summary = "전체공연목록, 홍보목록 조회 API", description = "홈화면에서 전체공연목록, 홍보목록을 조회하는 GET API입니다.")
    @GetMapping
    public ResponseEntity<SuccessResponse<HomeResponse>> getHomePerformanceList(@RequestParam(required = false) String genre) {
        HomeRequest homeRequest;
        if (genre != null) {
            homeRequest = new HomeRequest(Genre.valueOf(genre));
        } else {
            homeRequest = new HomeRequest(null);
        }        HomeResponse homeResponse = performanceService.getHomePerformanceList(homeRequest);
        return ResponseEntity.ok(SuccessResponse.of(PerformanceSuccessCode.HOME_PERFORMANCE_RETRIEVE_SUCCESS, homeResponse));
    }
}