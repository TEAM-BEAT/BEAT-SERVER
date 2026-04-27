package com.beat.infra.persistence.user.mapper;

import org.springframework.stereotype.Component;

import com.beat.domain.user.domain.Users;
import com.beat.infra.persistence.user.entity.UsersJpaEntity;

@Component
public class UsersPersistenceMapper {

	public Users toDomain(UsersJpaEntity entity) {
		return Users.rehydrate(
			entity.getId(),
			entity.getRole()
		);
	}

	public UsersJpaEntity toEntity(Users domain) {
		return UsersJpaEntity.rehydrate(
			domain.getId(),
			domain.getRole()
		);
	}
}
