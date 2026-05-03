package com.beat.gateway.security.internal.servlet;

import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class MemberAuthentication extends UsernamePasswordAuthenticationToken {

	public MemberAuthentication(
		Object principal,
		Object credentials,
		Collection<? extends GrantedAuthority> authorities
	) {
		super(principal, credentials, authorities);
	}
}
