package com.beat.domain.user.repository

import com.beat.domain.user.domain.Users
import java.util.*

@JvmSuppressWildcards
interface UserRepository {
    fun findById(id: Long?): Optional<Users>

    fun findAll(): List<Users>

    fun save(users: Users): Users

    fun delete(users: Users)
}
