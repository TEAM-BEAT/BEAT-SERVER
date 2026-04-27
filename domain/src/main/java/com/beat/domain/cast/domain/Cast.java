package com.beat.domain.cast.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cast {

	private Long id;

	private String castName;

	private String castRole;

	private String castPhoto;

	private Long performanceId;

	@Builder(access = AccessLevel.PRIVATE)
	private Cast(Long id, String castName, String castRole, String castPhoto, Long performanceId) {
		this.id = id;
		this.castName = castName;
		this.castRole = castRole;
		this.castPhoto = castPhoto;
		this.performanceId = performanceId;
	}

	public static Cast create(String castName, String castRole, String castPhoto, Long performanceId) {
		return Cast.builder()
			.castName(castName)
			.castRole(castRole)
			.castPhoto(castPhoto)
			.performanceId(performanceId)
			.build();
	}

	public static Cast rehydrate(Long id, String castName, String castRole, String castPhoto, Long performanceId) {
		return Cast.builder()
			.id(id)
			.castName(castName)
			.castRole(castRole)
			.castPhoto(castPhoto)
			.performanceId(performanceId)
			.build();
	}

	public void update(String castName, String castRole, String castPhoto) {
		this.castName = castName;
		this.castRole = castRole;
		this.castPhoto = castPhoto;
	}
}
