package com.beat.infra.redis.auth.guest

import org.springframework.data.repository.CrudRepository

interface GuestSessionRedisRepository : CrudRepository<GuestSessionRedisHash, String>
