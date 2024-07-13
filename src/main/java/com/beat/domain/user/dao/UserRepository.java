package com.beat.domain.user.dao;

import com.beat.domain.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
}
