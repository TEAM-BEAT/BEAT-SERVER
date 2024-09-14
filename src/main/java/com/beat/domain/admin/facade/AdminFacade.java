package com.beat.domain.admin.facade;

import com.beat.domain.admin.application.dto.CarouselFindAllResponse;
import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.domain.admin.port.in.AdminUseCase;
import com.beat.domain.member.port.in.MemberUseCase;
import com.beat.domain.promotion.domain.Promotion;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.port.in.UserUseCase;
import com.beat.global.external.s3.application.dto.BannerPresignedUrlFindResponse;
import com.beat.global.external.s3.application.dto.CarouselPresignedUrlFindAllResponse;
import com.beat.global.external.s3.port.in.FileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminFacade {
    private final FileUseCase fileUseCase;
    private final AdminUseCase adminUsecase;
    private final MemberUseCase memberUseCase;
    private final UserUseCase userUseCase;

    public UserFindAllResponse checkMemberAndFindAllUsers(Long memberId) {
        memberUseCase.findMemberById(memberId);
        List<Users> users = userUseCase.findAllUsers();
        return UserFindAllResponse.from(users);
    }

    public CarouselPresignedUrlFindAllResponse checkMemberAndIssueAllPresignedUrlsForCarousel(Long memberId, List<String> carouselImages) {
        memberUseCase.findMemberById(memberId);
        Map<String, String> carouselPresignedUrls = fileUseCase.issueAllPresignedUrlsForCarousel(carouselImages);
        return CarouselPresignedUrlFindAllResponse.from(carouselPresignedUrls);
    }

    public BannerPresignedUrlFindResponse checkMemberAndIssuePresignedUrlForBanner(Long memberId, String bannerImage) {
        memberUseCase.findMemberById(memberId);
        String bannerPresignedUrl = fileUseCase.issuePresignedUrlForBanner(bannerImage);
        return BannerPresignedUrlFindResponse.from(bannerPresignedUrl);
    }

    public CarouselFindAllResponse checkMemberAndFindAllPromotionsSortedByCarouselNumber(Long memberId) {
        memberUseCase.findMemberById(memberId);
        List<Promotion> promotions = adminUsecase.findAllPromotionsSortedByCarouselNumber();
        return CarouselFindAllResponse.from(promotions);
    }
}