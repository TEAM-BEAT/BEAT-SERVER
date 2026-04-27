package com.beat.infra.persistence.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.beat.domain.user.domain.Users;
import com.beat.domain.user.repository.UserRepository;
import com.beat.infra.persistence.user.entity.UsersJpaEntity;
import com.beat.infra.persistence.user.mapper.UsersPersistenceMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UsersRepositoryImpl implements UserRepository {

	private final UsersJpaRepository usersJpaRepository;
	private final UsersPersistenceMapper usersPersistenceMapper;

	@Override
	public Optional<Users> findById(Long id) {
		return usersJpaRepository.findById(id)
			.map(usersPersistenceMapper::toDomain);
	}

	@Override
	public List<Users> findAll() {
		return usersJpaRepository.findAll().stream()
			.map(usersPersistenceMapper::toDomain)
			.toList();
	}

	@Override
	public Users save(Users users) {
		UsersJpaEntity entity = usersPersistenceMapper.toEntity(users);
		UsersJpaEntity savedEntity = usersJpaRepository.save(entity);
		return usersPersistenceMapper.toDomain(savedEntity);
	}

	@Override
	public void delete(Users users) {
		if (users.getId() != null) {
			usersJpaRepository.deleteById(users.getId());
		}
	}
}
