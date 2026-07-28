package com.beat.infra.auth.redis.guest

import org.springframework.data.repository.CrudRepository

interface GuestSessionRedisRepository : CrudRepository<GuestSessionRedisHash, String>
