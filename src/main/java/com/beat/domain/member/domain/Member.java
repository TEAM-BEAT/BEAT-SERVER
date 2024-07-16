package com.beat.domain.member.domain;

import com.beat.domain.BaseTimeEntity;
import com.beat.domain.user.domain.Users;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @Column(nullable = false)
    private Long socialId;  // 카카오 회원번호 저장

    @Enumerated(EnumType.STRING)
    private SocialType socialType;

    @Builder
    public Member(String nickname, String email, LocalDateTime deletedAt, Users user, Long socialId, SocialType socialType) {
        this.nickname = nickname;
        this.email = email;
        this.deletedAt = deletedAt;
        this.user = user;
        this.socialId = socialId;
        this.socialType = socialType;
    }

    public static Member create(
            final String nickname,
            final String email,
            final Users user,
            final Long socialId,
            final SocialType socialType
    ) {
        return Member.builder()
                .nickname(nickname)
                .email(email)
                .user(user)
                .socialId(socialId)
                .socialType(socialType)
                .build();
    }
}
