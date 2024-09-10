package com.beat.domain.admin.application;

import com.beat.domain.admin.application.dto.UserFindAllResponse;
import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.exception.MemberErrorCode;
import com.beat.domain.user.dao.UserRepository;
import com.beat.global.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    public UserFindAllResponse findAllUsers(Long memberId) {
        memberRepository.findById(memberId)
                .ifPresentOrElse(member -> {},
                        () -> {throw new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND);});
        return UserFindAllResponse.of(userRepository.findAll());
    }
}