package com.beat.domain.user.repository

import com.beat.domain.user.model.Users

interface UserRepository {
    fun findById(id: Long): Users?

    fun findAll(): List<Users>

    fun save(users: Users): Users

    fun delete(users: Users)
}
