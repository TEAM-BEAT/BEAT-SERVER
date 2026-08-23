package com.beat.infra.support

import com.beat.infra.config.JpaConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime
import java.time.ZoneId

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
class MySqlSessionTimeZoneIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        test("설정된 Seoul 세션 타임존을 사용한다") {
            jdbcTemplate.queryForObject("SELECT @@session.time_zone", String::class.java) shouldBe "+09:00"
        }

        test("현재 타임스탬프를 감싸는 Seoul 벽시계 구간 안에서 반환한다") {
            val zone = ZoneId.of("Asia/Seoul")
            val beforeQuery = LocalDateTime.now(zone)
            val databaseNow = jdbcTemplate.queryForObject(
                "SELECT CURRENT_TIMESTAMP(6)",
                LocalDateTime::class.java,
            )!!
            val afterQuery = LocalDateTime.now(zone)
            val allowedStart = beforeQuery.minusSeconds(1)
            val allowedEnd = afterQuery.plusSeconds(1)

            (databaseNow >= allowedStart && databaseNow <= allowedEnd).shouldBeTrue()
        }
    }
}
