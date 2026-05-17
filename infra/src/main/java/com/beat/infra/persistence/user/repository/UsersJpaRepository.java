package com.beat.infra.persistence.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beat.infra.persistence.user.entity.UsersJpaEntity;

public interface UsersJpaRepository extends JpaRepository<UsersJpaEntity, Long> {
}
