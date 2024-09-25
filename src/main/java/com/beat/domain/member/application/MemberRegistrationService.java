package com.beat.domain.member.application;

import com.beat.domain.member.dao.MemberRepository;
import com.beat.domain.member.domain.Member;
import com.beat.domain.user.dao.UserRepository;
import com.beat.domain.user.domain.Role;
import com.beat.domain.user.domain.Users;
import com.beat.global.auth.client.dto.MemberInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberRegistrationService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long registerMemberWithUserInfo(final MemberInfoResponse memberInfoResponse) {
        Users users = Users.createWithRole(Role.MEMBER);
        users = userRepository.save(users);

        Member member = Member.create(
                memberInfoResponse.nickname(),
                memberInfoResponse.email(),
                users,
                memberInfoResponse.socialId(),
                memberInfoResponse.socialType()
        );

        memberRepository.save(member);
        return member.getId();
    }
}