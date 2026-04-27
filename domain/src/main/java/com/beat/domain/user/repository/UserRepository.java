package com.beat.domain.user.repository;

import java.util.List;
import java.util.Optional;

import com.beat.domain.user.domain.Users;

public interface UserRepository {
	Optional<Users> findById(Long id);

	List<Users> findAll();

	Users save(Users users);

	void delete(Users users);
}
