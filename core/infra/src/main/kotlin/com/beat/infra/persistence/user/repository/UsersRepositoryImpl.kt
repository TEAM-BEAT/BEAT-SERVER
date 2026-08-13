package com.beat.infra.persistence.user.repository

import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import com.beat.infra.persistence.user.mapper.UsersPersistenceMapper
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class UsersRepositoryImpl(
    private val usersJpaRepository: UsersJpaRepository,
    private val usersPersistenceMapper: UsersPersistenceMapper,
) : UserRepository {
    override fun findById(id: Long?): Optional<Users> =
        usersJpaRepository.findById(requireNotNull(id) { "The given id must not be null" })
            .map(usersPersistenceMapper::toDomain)

    override fun findAll(): List<Users> =
        usersJpaRepository.findAll().map(usersPersistenceMapper::toDomain)

    override fun save(users: Users): Users =
        usersPersistenceMapper.toDomain(usersJpaRepository.save(usersPersistenceMapper.toEntity(users)))

    override fun delete(users: Users) {
        users.getId()?.let(usersJpaRepository::deleteById)
    }
}
