package com.beat.apps.api.booking.web

/** 게스트 세션의 HTTP 계약 상수. 발급(BookingController)과 검증(GuestSessionOriginFilter)이 공유하는 단일 소스다. */
const val GUEST_SESSION_COOKIE_NAME: String = "__Host-guestSession"
