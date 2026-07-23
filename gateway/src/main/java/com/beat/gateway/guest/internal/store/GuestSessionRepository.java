package com.beat.gateway.guest.internal.store;

import org.springframework.data.repository.CrudRepository;

public interface GuestSessionRepository extends CrudRepository<GuestSession, String> {
}
