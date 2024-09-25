package com.beat.domain.user.port.in;

import com.beat.domain.user.domain.Users;

import java.util.List;

public interface UserUseCase {
    List<Users> findAllUsers();
}