package com.beat.global.auth.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class AdminAuthentication extends UsernamePasswordAuthenticationToken {

	public AdminAuthentication(Object principal, Object credentials,
		Collection<? extends GrantedAuthority> authorities) {
		super(principal, credentials, authorities);
	}

	public Long getAdminId() {
		return (Long)getPrincipal();
	}
}