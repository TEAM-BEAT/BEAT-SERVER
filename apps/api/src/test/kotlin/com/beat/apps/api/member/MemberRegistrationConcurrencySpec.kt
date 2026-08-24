package com.beat.apps.api.member

import com.beat.apps.api.ApisApplication
import com.beat.apps.api.support.BeatTestContainersConfig
import com.beat.application.frontoffice.member.command.LoginSuccessResult
import com.beat.application.frontoffice.member.command.SocialLoginCommand
import com.beat.application.frontoffice.member.command.SocialLoginCommandService
import com.beat.application.frontoffice.member.command.SocialLoginProfile
import com.beat.application.frontoffice.member.command.SocialLoginProvider
import com.beat.application.frontoffice.member.command.SocialLoginType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.user.repository.UserRepository
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [ApisApplication::class])
@ActiveProfiles("test")
@Import(BeatTestContainersConfig::class, MemberRegistrationConcurrencySpec.TestConfig::class)
@Tags("correctness")
open class MemberRegistrationConcurrencySpec : FunSpec() {
    @Autowired
    private lateinit var socialLoginCommandService: SocialLoginCommandService

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("동일 Social identity 동시 로그인은 한 회원만 등록하고 두 요청 모두 정상 반환한다") {
            val socialIdentity = SocialIdentity.of(SocialType.KAKAO, SOCIAL_ID)
            val usersBefore = userRepository.findAll().size
            val ready = CountDownLatch(2)
            val start = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)

            try {
                val futures = listOf(
                    executor.submit<LoginSuccessResult> {
                        awaitStart(ready, start)
                        socialLoginCommandService.handleSocialLogin(
                            authorizationCode = "authorization-code-1",
                            command = SocialLoginCommand(SocialLoginType.KAKAO),
                        )
                    },
                    executor.submit<LoginSuccessResult> {
                        awaitStart(ready, start)
                        socialLoginCommandService.handleSocialLogin(
                            authorizationCode = "authorization-code-2",
                            command = SocialLoginCommand(SocialLoginType.KAKAO),
                        )
                    },
                )
                check(ready.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                start.countDown()

                val results = futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }

                results.map { it.nickname }.distinct() shouldBe listOf("concurrent-member")
                countMembers(socialIdentity) shouldBe 1
                userRepository.findAll().size shouldBe usersBefore + 1
                checkNotNull(memberRepository.findBySocialIdentity(socialIdentity)).socialIdentity shouldBe socialIdentity
            } finally {
                executor.shutdownNow()
                check(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            }
        }
    }

    private fun awaitStart(ready: CountDownLatch, start: CountDownLatch) {
        ready.countDown()
        check(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    private fun countMembers(socialIdentity: SocialIdentity): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM member WHERE social_type = ? AND social_id = ?",
        Int::class.java,
        socialIdentity.socialType.name,
        socialIdentity.socialId,
    ) ?: 0

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        @Primary
        fun socialLoginProvider(): SocialLoginProvider = SocialLoginProvider {
            SocialLoginProfile(
                socialId = SOCIAL_ID,
                nickname = "concurrent-member",
                email = "member@example.com",
            )
        }

    }
}

private const val SOCIAL_ID = 991_627_341L
private const val TIMEOUT_SECONDS = 10L
