package com.beat.infra.persistence.member.repository

import com.beat.domain.member.exception.DuplicateSocialIdentityException
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.infra.config.JpaConfig
import com.beat.infra.support.MySqlTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest(
    properties = [
        "spring.config.import=classpath:application-persistence.yml",
        "DB_HIKARI_MAX_POOL_SIZE=10",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [JpaConfig::class, MySqlTestContainerConfig::class])
@ActiveProfiles("test")
@Tags("integration")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MemberRepositoryMySqlIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var memberJpaRepository: MemberJpaRepository

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        afterTest { memberJpaRepository.deleteAllInBatch() }

        test("MySQL social identity 제약을 변환하고 첫 member를 보존한다") {
            val socialIdentity = SocialIdentity.of(SocialType.KAKAO, 2026082201L)
            val first = memberRepository.save(
                Member.create(
                    nickname = "first-member",
                    email = "first@example.com",
                    userId = 101L,
                    socialIdentity = socialIdentity,
                ),
            )

            shouldThrow<DuplicateSocialIdentityException> {
                memberRepository.save(
                    Member.create(
                        nickname = "second-member",
                        email = "second@example.com",
                        userId = 102L,
                        socialIdentity = socialIdentity,
                    ),
                )
            }

            memberRepository.count() shouldBe 1L
            checkNotNull(memberRepository.findBySocialIdentity(socialIdentity)).userId shouldBe first.userId
        }
    }
}
