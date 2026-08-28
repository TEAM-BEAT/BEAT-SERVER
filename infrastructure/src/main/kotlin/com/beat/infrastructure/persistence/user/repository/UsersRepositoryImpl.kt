package com.beat.infrastructure.persistence.user.repository

import com.beat.domain.user.model.Users
import com.beat.domain.user.repository.UserRepository
import com.beat.infrastructure.persistence.user.mapper.UsersPersistenceMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
internal class UsersRepositoryImpl(
    private val usersJpaRepository: UsersJpaRepository,
    private val usersPersistenceMapper: UsersPersistenceMapper,
) : UserRepository {
    override fun findById(id: Long): Users? =
        usersJpaRepository.findByIdOrNull(id)?.let(usersPersistenceMapper::toDomain)

    override fun findAll(): List<Users> =
        usersJpaRepository.findAll().map(usersPersistenceMapper::toDomain)

    override fun save(users: Users): Users =
        usersPersistenceMapper.toDomain(
            usersJpaRepository.save(usersPersistenceMapper.toEntity(users))
        )

    override fun delete(users: Users) {
        users.id?.let(usersJpaRepository::deleteById)
    }
}
