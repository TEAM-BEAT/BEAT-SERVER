package com.beat.global.external.s3.port.in;

import com.beat.global.external.s3.application.dto.BannerPresignedUrlFindResponse;
import com.beat.global.external.s3.application.dto.CarouselPresignedUrlFindAllResponse;
import com.beat.global.external.s3.application.dto.PerformanceMakerPresignedUrlFindAllResponse;

import java.util.List;
import java.util.Map;

public interface FileUseCase {

    PerformanceMakerPresignedUrlFindAllResponse issueAllPresignedUrlsForPerformanceMaker(String posterImage, List<String> castImages, List<String> staffImages, List<String> performanceImages);

    Map<String, String> issueAllPresignedUrlsForCarousel(List<String> carouselImages);

    String issuePresignedUrlForBanner(String bannerImage);
}