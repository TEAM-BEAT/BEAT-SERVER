package com.beat.infra.persistence.cast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "Cast")
@Table(name = "cast")
public class CastJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false)
	private Long id;

	@Column(nullable = false)
	private String castName;

	@Column(nullable = false)
	private String castRole;

	@Column(nullable = false)
	private String castPhoto;

	@Column(name = "performance_id", nullable = false)
	private Long performanceId;

	protected CastJpaEntity() {
	}

	private CastJpaEntity(Long id, String castName, String castRole, String castPhoto, Long performanceId) {
		this.id = id;
		this.castName = castName;
		this.castRole = castRole;
		this.castPhoto = castPhoto;
		this.performanceId = performanceId;
	}

	public static CastJpaEntity rehydrate(Long id, String castName, String castRole, String castPhoto,
		Long performanceId) {
		return new CastJpaEntity(id, castName, castRole, castPhoto, performanceId);
	}

	public Long getId() {
		return id;
	}

	public String getCastName() {
		return castName;
	}

	public String getCastRole() {
		return castRole;
	}

	public String getCastPhoto() {
		return castPhoto;
	}

	public Long getPerformanceId() {
		return performanceId;
	}
}
