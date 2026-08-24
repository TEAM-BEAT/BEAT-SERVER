package com.beat.infrastructure.support

import com.beat.infrastructure.config.JpaConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

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

        test("현재 타임스탬프는 UTC 기준보다 정확히 9시간 앞선다") {
            val offsetSeconds = jdbcTemplate.queryForObject(
                "SELECT TIMESTAMPDIFF(SECOND, UTC_TIMESTAMP(6), CURRENT_TIMESTAMP(6))",
                Long::class.java,
            )

            offsetSeconds shouldBe 32_400L
        }
    }
}
