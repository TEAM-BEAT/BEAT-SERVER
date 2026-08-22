package com.beat.support.security.password.internal

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BCryptPasswordHasherTest : FunSpec() {

    private val passwordHasher = BCryptPasswordHasher()

    init {
        isolationMode = IsolationMode.SingleInstance

        test("bcrypt password matches only the original password") {
            val encoded = passwordHasher.encode("secret")

            passwordHasher.matches("secret", encoded) shouldBe true
            passwordHasher.matches("wrong", encoded) shouldBe false
            passwordHasher.needsUpgrade(encoded) shouldBe false
        }

        test("blank stored password is rejected") {
            passwordHasher.matches("secret", "") shouldBe false
            passwordHasher.matches("secret", "   ") shouldBe false
        }

        test("legacy plaintext password is supported and marked for upgrade") {
            passwordHasher.matches("legacy", "legacy") shouldBe true
            passwordHasher.matches("wrong", "legacy") shouldBe false
            passwordHasher.needsUpgrade("legacy") shouldBe true
        }

        test("encoding produces a new bcrypt password") {
            val encoded = passwordHasher.encode("secret")

            encoded.startsWith("\$2") shouldBe true
            encoded shouldNotBe "secret"
        }
    }
}
