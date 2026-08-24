package com.beat.infrastructure.persistence.promotion.repository

import com.beat.domain.performance.model.Genre
import com.beat.domain.promotion.model.CarouselNumber
import com.beat.domain.promotion.repository.PromotionRepository
import com.beat.infrastructure.config.JpaConfig
import com.beat.infrastructure.persistence.performance.entity.PerformanceJpaEntity
import com.beat.infrastructure.persistence.performance.entity.PerformancePeriodJpaValue
import com.beat.infrastructure.persistence.performance.repository.PerformanceJpaRepository
import com.beat.infrastructure.persistence.promotion.entity.PromotionJpaEntity
import com.beat.infrastructure.support.MySqlTestContainerConfig
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.mysql.MySQLContainer
import java.sql.DriverManager
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

@DataJpaTest(
    properties = [
        "spring.config.import=classpath:application-persistence.yml",
        "DB_HIKARI_MAX_POOL_SIZE=10",
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [JpaConfig::class, MySqlTestContainerConfig::class])
@ActiveProfiles("test")
@Tags("correctness")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PromotionLockingIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var mysqlContainer: MySQLContainer

    @Autowired
    private lateinit var performanceRepository: PerformanceJpaRepository

    @Autowired
    private lateinit var promotionJpaRepository: PromotionJpaRepository

    @Autowired
    private lateinit var promotionRepository: PromotionRepository

    init {
        isolationMode = IsolationMode.SingleInstance
        extension(SpringExtension(SpringTestLifecycleMode.Test))

        afterTest {
            transaction().executeWithoutResult {
                promotionJpaRepository.deleteAllInBatch()
                performanceRepository.deleteAllInBatch()
            }
        }

        test("carousel 일괄 변경은 기존 Promotion row lock이 해제될 때까지 직렬화된다") {
            transaction().executeWithoutResult {
                promotionJpaRepository.save(promotion(CarouselNumber.ONE, null))
            }
            val firstLocked = CountDownLatch(1)
            val releaseFirst = CountDownLatch(1)
            val secondLocked = CountDownLatch(1)

            Executors.newFixedThreadPool(2).use { executor ->
                val first = executor.submit {
                    transaction().executeWithoutResult {
                        promotionRepository.lockAll()
                        firstLocked.countDown()
                        check(releaseFirst.await(5, TimeUnit.SECONDS))
                    }
                }
                check(firstLocked.await(5, TimeUnit.SECONDS))
                val second = executor.submit {
                    transaction().executeWithoutResult {
                        promotionRepository.lockAll()
                        secondLocked.countDown()
                    }
                }

                awaitNamedLockWait()
                secondLocked.count shouldBe 1L
                releaseFirst.countDown()
                first.get(5, TimeUnit.SECONDS)
                second.get(5, TimeUnit.SECONDS)
                secondLocked.count shouldBe 0L
            }
        }

        test("Promotion이 비어 있어도 carousel namespace lock이 다음 변경을 직렬화한다") {
            val firstLocked = CountDownLatch(1)
            val releaseFirst = CountDownLatch(1)
            val secondLocked = CountDownLatch(1)

            Executors.newFixedThreadPool(2).use { executor ->
                val first = executor.submit {
                    transaction().executeWithoutResult {
                        promotionRepository.lockAll()
                        firstLocked.countDown()
                        check(releaseFirst.await(5, TimeUnit.SECONDS))
                    }
                }
                check(firstLocked.await(5, TimeUnit.SECONDS))
                val second = executor.submit {
                    transaction().executeWithoutResult {
                        promotionRepository.lockAll()
                        secondLocked.countDown()
                    }
                }

                awaitNamedLockWait()
                secondLocked.count shouldBe 1L
                releaseFirst.countDown()
                first.get(5, TimeUnit.SECONDS)
                second.get(5, TimeUnit.SECONDS)
            }
        }

        test("Promotion 생성과 Performance 삭제가 같은 authoritative Performance lock으로 직렬화된다") {
            val performanceId = transaction().execute {
                performanceRepository.save(performance()).id!!
            }
            val creatorLocked = CountDownLatch(1)
            val releaseCreator = CountDownLatch(1)
            val deleterLocked = CountDownLatch(1)

            Executors.newFixedThreadPool(2).use { executor ->
                val creator = executor.submit {
                    transaction().executeWithoutResult {
                        checkNotNull(performanceRepository.lockById(performanceId))
                        creatorLocked.countDown()
                        check(releaseCreator.await(5, TimeUnit.SECONDS))
                        promotionJpaRepository.save(promotion(CarouselNumber.ONE, performanceId))
                    }
                }
                check(creatorLocked.await(5, TimeUnit.SECONDS))
                val deleter = executor.submit {
                    transaction().executeWithoutResult {
                        checkNotNull(performanceRepository.lockById(performanceId))
                        deleterLocked.countDown()
                        promotionJpaRepository.deleteByPerformanceId(performanceId)
                        performanceRepository.deleteById(performanceId)
                    }
                }

                awaitRowLockWait()
                deleterLocked.count shouldBe 1L
                releaseCreator.countDown()
                creator.get(5, TimeUnit.SECONDS)
                deleter.get(5, TimeUnit.SECONDS)
            }

            promotionJpaRepository.count() shouldBe 0L
            performanceRepository.existsById(performanceId) shouldBe false
        }
    }

    private fun transaction(): TransactionTemplate = TransactionTemplate(transactionManager)

    private fun awaitNamedLockWait() {
        awaitDatabaseWait(
            """
                SELECT COUNT(*)
                FROM performance_schema.metadata_locks
                WHERE OBJECT_TYPE = 'USER LEVEL LOCK'
                  AND OBJECT_NAME = 'beat:promotion:carousel'
                  AND LOCK_STATUS = 'PENDING'
            """.trimIndent(),
        )
    }

    private fun awaitRowLockWait() {
        awaitDatabaseWait("SELECT COUNT(*) FROM performance_schema.data_lock_waits")
    }

    private fun awaitDatabaseWait(sql: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        DriverManager.getConnection(mysqlContainer.jdbcUrl, "root", mysqlContainer.password).use { connection ->
            while (System.nanoTime() < deadline) {
                connection.createStatement().use { statement ->
                    statement.executeQuery(sql).use { result ->
                        result.next()
                        if (result.getInt(1) > 0) {
                            return
                        }
                    }
                }
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10))
            }
        }
        error("Timed out waiting for database lock state")
    }
}

private fun promotion(carouselNumber: CarouselNumber, performanceId: Long?): PromotionJpaEntity =
    PromotionJpaEntity.rehydrate(
        id = null,
        promotionPhoto = "image-${carouselNumber.name.lowercase()}",
        performanceId = performanceId,
        redirectUrl = "https://beat.example/${carouselNumber.name.lowercase()}",
        isExternal = false,
        carouselNumber = carouselNumber,
    )

private fun performance(): PerformanceJpaEntity = PerformanceJpaEntity.rehydrate(
    id = null,
    performanceTitle = "promotion-lock-performance",
    genre = Genre.PLAY,
    runningTime = 90,
    performanceDescription = "description",
    performanceAttentionNote = "attention",
    paymentAccount = null,
    posterImage = "poster.png",
    performanceTeamName = "team",
    performanceVenue = "venue",
    roadAddressName = "road",
    placeDetailAddress = "detail",
    latitude = "37.0",
    longitude = "127.0",
    performanceContact = "010-0000-0000",
    performancePeriodValue = PerformancePeriodJpaValue(
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 8, 2),
    ),
    legacyPerformancePeriod = "2026-08-01~2026-08-02",
    ticketPrice = 10_000,
    totalScheduleCount = 0,
    userId = 1L,
)
