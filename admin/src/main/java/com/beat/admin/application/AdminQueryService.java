package com.beat.admin.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beat.admin.application.dto.response.BannerPresignedUrlFindResponse;
import com.beat.admin.application.dto.response.CarouselFindAllResponse;
import com.beat.admin.application.dto.response.CarouselPresignedUrlFindAllResponse;
import com.beat.admin.application.dto.response.UserFindAllResponse;
import com.beat.admin.application.dto.result.AdminUserResult;
import com.beat.admin.application.exception.AdminApplicationErrorCode;
import com.beat.contracts.storage.FileStoragePort;
import com.beat.domain.member.repository.MemberRepository;
import com.beat.domain.promotion.repository.PromotionRepository;
import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.global.common.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQueryService {

	private final FileStoragePort fileStoragePort;
	private final MemberRepository memberRepository;
	private final UserRepository userRepository;
	private final PromotionRepository promotionRepository;

	public UserFindAllResponse findAllUsers(Long memberId) {
		validateMemberExists(memberId);
		List<AdminUserResult> users = userRepository.findAll().stream()
			.map(this::toUserResult)
			.toList();
		return UserFindAllResponse.from(users);
	}

	public CarouselPresignedUrlFindAllResponse issueAllPresignedUrlsForCarousel(Long memberId,
		List<String> carouselImages) {
		validateMemberExists(memberId);
		return CarouselPresignedUrlFindAllResponse.from(
			fileStoragePort.issueAllPresignedUrlsForCarousel(carouselImages)
		);
	}

	public BannerPresignedUrlFindResponse issuePresignedUrlForBanner(Long memberId, String bannerImage) {
		validateMemberExists(memberId);
		return BannerPresignedUrlFindResponse.from(fileStoragePort.issuePresignedUrlForBanner(bannerImage));
	}

	public CarouselFindAllResponse findAllPromotionsSortedByCarouselNumber(Long memberId) {
		validateMemberExists(memberId);
		return CarouselFindAllResponse.from(
			AdminPromotionResults.fromSortedByCarouselNumber(promotionRepository.findAll())
		);
	}

	private AdminUserResult toUserResult(Users user) {
		return new AdminUserResult(
			user.getId(),
			user.getRole().getRoleName()
		);
	}

	private void validateMemberExists(Long memberId) {
		memberRepository.findById(memberId)
			.orElseThrow(() -> new NotFoundException(AdminApplicationErrorCode.MEMBER_NOT_FOUND));
	}
}
