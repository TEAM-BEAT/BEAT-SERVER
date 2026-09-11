package com.beat.apps.api.booking.experiment

import com.beat.apps.api.ApisApplication
import com.beat.apps.api.fixture.performanceFixture
import com.beat.apps.api.fixture.scheduleFixture
import com.beat.apps.api.fixture.usersFixture
import com.beat.apps.api.support.BeatTestContainersConfig
import com.beat.domain.booking.repository.BookingRepository
import com.beat.domain.member.model.Member
import com.beat.domain.member.model.SocialType
import com.beat.domain.member.repository.MemberRepository
import com.beat.domain.member.vo.SocialIdentity
import com.beat.domain.performance.repository.PerformanceRepository
import com.beat.domain.performance.vo.PerformancePeriod
import com.beat.domain.schedule.model.ScheduleNumber
import com.beat.domain.schedule.repository.ScheduleRepository
import com.beat.domain.user.repository.UserRepository
import com.beat.infrastructure.booking.booker.experiment.StockContentionScheduleVersionPrerequisite
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    classes = [ApisApplication::class],
    properties =
        [
            "booking.experiment.enabled=true",
            "spring.datasource.hikari.maximum-pool-size=1",
            "spring.datasource.hikari.connection-timeout=500",
        ],
)
@ActiveProfiles("dev", "test")
@Import(BeatTestContainersConfig::class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StockContentionConnectionBoundaryIntegrationSpec : FunSpec() {

    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var dataSource: DataSource

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var memberRepository: MemberRepository

    @Autowired private lateinit var performanceRepository: PerformanceRepository

    @Autowired private lateinit var scheduleRepository: ScheduleRepository

    @Autowired private lateinit var bookingRepository: BookingRepository

    @MockitoBean
    private lateinit var scheduleVersionPrerequisite: StockContentionScheduleVersionPrerequisite

    init {
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("connection pool이 하나여도 준비 transaction 반환 후 비관적 예매 transaction이 성공한다") {
            val fixture = createFixture()
            dataSource.unwrap(HikariDataSource::class.java).maximumPoolSize shouldBe 1

            mockMvc
                .perform(
                    post("/api/internal/experiments/stock-contention/PESSIMISTIC/bookings")
                        .with(authentication(memberAuthentication(fixture.memberId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "scheduleId": ${fixture.scheduleId},
                              "purchaseTicketCount": 1,
                              "bookerName": "회귀검증예매자",
                              "bookerPhoneNumber": "010-1234-5678"
                            }
                            """
                                .trimIndent()
                        )
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.outcome").value("ACCEPTED"))
                .andExpect(jsonPath("$.bookingId").isNumber)

            checkNotNull(scheduleRepository.findById(fixture.scheduleId))
                .allocatedTicketCount shouldBe 1
            bookingRepository.findAll().count { it.scheduleId == fixture.scheduleId } shouldBe 1
        }
    }

    private fun createFixture(): Fixture {
        val databaseNow =
            checkNotNull(
                jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", LocalDateTime::class.java)
            )
        val user = userRepository.save(usersFixture())
        val userId = requireNotNull(user.id)
        val member =
            memberRepository.save(
                Member.create(
                    nickname = "connection-boundary-$userId",
                    email = "connection-boundary-$userId@example.com",
                    userId = userId,
                    socialIdentity = SocialIdentity.of(SocialType.KAKAO, 8_000_000_000L + userId),
                )
            )
        val performance =
            performanceRepository.save(
                performanceFixture(
                    userId = userId,
                    ticketPrice = 0,
                    performancePeriod =
                        PerformancePeriod.of(
                            databaseNow.toLocalDate(),
                            databaseNow.plusDays(2).toLocalDate(),
                        ),
                    totalScheduleCount = 1,
                )
            )
        val schedule =
            scheduleRepository.save(
                scheduleFixture(
                    performanceId = requireNotNull(performance.id),
                    performanceDate = databaseNow.plusDays(1),
                    bookingCloseAt = databaseNow.plusDays(1).plusHours(2),
                    totalTicketCount = 2,
                    scheduleNumber = ScheduleNumber.FIRST,
                )
            )
        return Fixture(
            memberId = requireNotNull(member.id),
            scheduleId = requireNotNull(schedule.id),
        )
    }

    private fun memberAuthentication(memberId: Long) =
        UsernamePasswordAuthenticationToken(
            memberId,
            null,
            listOf(SimpleGrantedAuthority("ROLE_MEMBER")),
        )

    private data class Fixture(val memberId: Long, val scheduleId: Long)
}
