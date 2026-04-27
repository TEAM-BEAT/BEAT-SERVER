package com.beat.infra.persistence.staff.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "Staff")
@Table(name = "staff")
public class StaffJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(nullable = false)
	private String staffName;

	@Column(nullable = false)
	private String staffRole;

	@Column(nullable = false)
	private String staffPhoto;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	protected StaffJpaEntity() {
	}

	private StaffJpaEntity(Long id, String staffName, String staffRole, String staffPhoto, Long performanceId) {
		this.id = id;
		this.staffName = staffName;
		this.staffRole = staffRole;
		this.staffPhoto = staffPhoto;
		this.performanceId = performanceId;
	}

	public static StaffJpaEntity rehydrate(Long id, String staffName, String staffRole, String staffPhoto,
		Long performanceId) {
		return new StaffJpaEntity(id, staffName, staffRole, staffPhoto, performanceId);
	}

	public Long getId() {
		return id;
	}

	public String getStaffName() {
		return staffName;
	}

	public String getStaffRole() {
		return staffRole;
	}

	public String getStaffPhoto() {
		return staffPhoto;
	}

	public Long getPerformanceId() {
		return performanceId;
	}
}
