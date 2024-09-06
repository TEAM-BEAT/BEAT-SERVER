package com.beat.domain.admin.application;

import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.domain.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;

    public UserFindAllResponse findAllUsers() {
        return UserFindAllResponse.of(userRepository.findAll());
    }
}