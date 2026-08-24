package com.beat.infrastructure.redis.auth.guest

import org.springframework.data.repository.CrudRepository

internal interface GuestSessionRedisRepository : CrudRepository<GuestSessionRedisHash, String>
