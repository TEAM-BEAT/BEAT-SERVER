package com.beat.infra.persistence.user.repository

import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import com.beat.infra.persistence.user.mapper.UsersPersistenceMapper
import org.springframework.stereotype.Repository

@Repository
internal class UsersRepositoryImpl(
    private val usersJpaRepository: UsersJpaRepository,
    private val usersPersistenceMapper: UsersPersistenceMapper,
) : UserRepository {
    override fun findById(id: Long): Users? =
        usersJpaRepository.findById(id)
            .map(usersPersistenceMapper::toDomain).orElse(null)

    override fun findAll(): List<Users> =
        usersJpaRepository.findAll().map(usersPersistenceMapper::toDomain)

    override fun save(users: Users): Users =
        usersPersistenceMapper.toDomain(usersJpaRepository.save(usersPersistenceMapper.toEntity(users)))

    override fun delete(users: Users) {
        users.id?.let(usersJpaRepository::deleteById)
    }
}
