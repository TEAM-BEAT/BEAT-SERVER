package com.beat.domain.staff.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff {

	private Long id;

	private String staffName;

	private String staffRole;

	private String staffPhoto;

	private Long performanceId;

	@Builder(access = AccessLevel.PRIVATE)
	private Staff(Long id, String staffName, String staffRole, String staffPhoto, Long performanceId) {
		this.id = id;
		this.staffName = staffName;
		this.staffRole = staffRole;
		this.staffPhoto = staffPhoto;
		this.performanceId = performanceId;
	}

	public static Staff create(String staffName, String staffRole, String staffPhoto, Long performanceId) {
		return Staff.builder()
			.staffName(staffName)
			.staffRole(staffRole)
			.staffPhoto(staffPhoto)
			.performanceId(performanceId)
			.build();
	}

	public static Staff rehydrate(Long id, String staffName, String staffRole, String staffPhoto, Long performanceId) {
		return Staff.builder()
			.id(id)
			.staffName(staffName)
			.staffRole(staffRole)
			.staffPhoto(staffPhoto)
			.performanceId(performanceId)
			.build();
	}

	public void update(String staffName, String staffRole, String staffPhoto) {
		this.staffName = staffName;
		this.staffRole = staffRole;
		this.staffPhoto = staffPhoto;
	}
}
