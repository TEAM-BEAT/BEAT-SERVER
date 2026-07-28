package com.beat.gateway.guest.internal.store

import org.springframework.data.repository.CrudRepository

interface GuestSessionRepository : CrudRepository<GuestSession, String>
