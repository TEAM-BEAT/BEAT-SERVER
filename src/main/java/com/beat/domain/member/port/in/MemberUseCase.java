package com.beat.domain.member.port.in;

import com.beat.domain.member.domain.Member;

public interface MemberUseCase {
    Member findMemberById(Long memberId);
}
