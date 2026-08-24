package com.beat.support.security.password.internal

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BCryptPasswordHasherTest : FunSpec() {

    private val passwordHasher = BCryptPasswordHasher()

    init {
        isolationMode = IsolationMode.SingleInstance

        test("bcrypt 비밀번호는 원래 비밀번호와만 일치한다") {
            val encoded = passwordHasher.encode("secret")

            passwordHasher.matches("secret", encoded) shouldBe true
            passwordHasher.matches("wrong", encoded) shouldBe false
            passwordHasher.needsUpgrade(encoded) shouldBe false
        }

        test("빈 저장 비밀번호는 거부된다") {
            passwordHasher.matches("secret", "") shouldBe false
            passwordHasher.matches("secret", "   ") shouldBe false
        }

        test("레거시 평문 비밀번호는 지원되고 업그레이드 대상으로 표시된다") {
            passwordHasher.matches("legacy", "legacy") shouldBe true
            passwordHasher.matches("wrong", "legacy") shouldBe false
            passwordHasher.needsUpgrade("legacy") shouldBe true
        }

        test("인코딩은 새 bcrypt 비밀번호를 만든다") {
            val encoded = passwordHasher.encode("secret")

            encoded.startsWith("\$2") shouldBe true
            encoded shouldNotBe "secret"
        }
    }
}
