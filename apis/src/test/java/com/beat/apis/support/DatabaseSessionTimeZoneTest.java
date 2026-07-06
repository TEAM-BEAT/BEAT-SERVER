package com.beat.apis.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * hibernate.jdbc.time_zone=Asia/Seoul 세션 강제가 실제로 적용되는지 검증한다.
 *
 * <p>JVM 프로세스의 기본 시간대와 무관하게, JDBC 커넥션 세션의 {@code time_zone}이
 * KST로 강제되어 DB가 직접 생성하는 CURRENT_TIMESTAMP도 KST 벽시계 값을 반환해야 한다.
 * 이 계약이 깨지면 예매 마감 판정(schedule.performance_date + running_time)이
 * DB가 생성한 시각과 어긋나 최대 9시간의 오차가 발생한다.
 */
class DatabaseSessionTimeZoneTest extends AbstractIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Test
	void 커넥션_세션의_time_zone은_KST로_강제된다() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			JdbcTemplate jdbcTemplate = new JdbcTemplate();
			jdbcTemplate.setDataSource(dataSource);

			String sessionTimeZone = jdbcTemplate.queryForObject(
				"SELECT @@session.time_zone", String.class);

			assertThat(sessionTimeZone).isEqualTo("+09:00");
		}
	}

	@Test
	void DB가_생성한_현재시각은_KST_벽시계_기준이다() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(dataSource);

		LocalDateTime dbNow = jdbcTemplate.queryForObject(
			"SELECT CURRENT_TIMESTAMP(6)", LocalDateTime.class);
		LocalDateTime kstNow = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		// 테스트 실행 지연을 감안해 5초 이내 오차만 허용한다.
		assertThat(Duration.between(dbNow, kstNow).abs()).isLessThan(Duration.ofSeconds(5));
	}
}
