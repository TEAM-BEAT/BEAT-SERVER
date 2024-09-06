package com.beat.domain.admin.application;

import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.user.dao.UserRepository;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    public UserFindAllResponse findAllUsers(Long memberId) {

        memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));

        return UserFindAllResponse.of(userRepository.findAll());
    }
}