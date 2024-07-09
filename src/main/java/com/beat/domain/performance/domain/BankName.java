package com.beat.domain.performance.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BankName {
    NH_NONGHYUP("NH농협"),
    KAKAOBANK("카카오뱅크"),
    KB_KOOKMIN("KB국민"),
    TOSSBANK("토스뱅크"),
    SHINHAN("신한"),
    WOORI("우리"),
    IBK_GIUP("IBK기업"),
    HANA("하나"),
    SAEMAUL("새마을"),
    BUSAN("부산"),
    IMBANK_DAEGU("IM뱅크(대구)"),
    SINHYEOP("신협"),
    WOOCHAEGUK("우체국"),
    SCJEIL("SC제일"),
    SUHYEOP("수협");

    private final String displayName;
}