package com.beat.domain.member.domain;

import java.time.LocalDateTime;

import com.beat.domain.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nickname;

	@Column(nullable = true)
	private String email;

	@Column(nullable = true)
	private LocalDateTime deletedAt;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long socialId;  // 카카오 회원번호 저장

	@Enumerated(EnumType.STRING)
	private SocialType socialType;

	@Builder
	private Member(String nickname, String email, LocalDateTime deletedAt, Long userId, Long socialId,
		SocialType socialType) {
		this.nickname = nickname;
		this.email = email;
		this.deletedAt = deletedAt;
		this.userId = userId;
		this.socialId = socialId;
		this.socialType = socialType;
	}

	public static Member create(
		final String nickname,
		final String email,
		final Long userId,
		final Long socialId,
		final SocialType socialType
	) {
		return Member.builder()
			.nickname(nickname)
			.email(email)
			.userId(userId)
			.socialId(socialId)
			.socialType(socialType)
			.build();
	}
}
