-- =============================================================================
-- BEAT Kotlin 마이그레이션 선행 스키마 EXPAND — beatProd
-- MySQL 8.0 / 멱등(재실행 안전) / DDL·DML·DCL·TCL 만 사용 (클라이언트 메타 명령 없음)
-- =============================================================================
-- 이 파일은 ops/db/migration/2026_kotlin_migration_expand_dev.sql 과 반드시
-- 동일한 본문(STEP 0 ~ STEP 9)을 유지해야 한다. beatDev/beatProd 는 완전히
-- 분리된 별도 DB 경로이며 이 두 파일을 이어주는 접속 정보나 인클루드 지시어는
-- 존재하지 않는다. 본문을 수정할 때는 두 파일에 반드시 동일하게 반영할 것.
--
-- 이 파일이 하는 일 (EXPAND 단계만):
--   1. booking.total_payment_amount        컬럼 추가(NULL) + 기존행 백필
--   2. booking  환불계좌 3필드 all-or-none  CHECK 제약 추가
--   3. performance 결제계좌 정규화 + 기간(start/end DATE) 컬럼 추가/백필 + CHECK 2종
--   4. member    (user_id) / (social_type, social_id) UNIQUE 제약 추가
--
-- 이 파일이 하지 않는 일 (CONTRACT 단계 — 나중에 별도 실행):
--   - total_payment_amount / performance_start_date / performance_end_date 를
--     NOT NULL 로 조이는 작업은 포함하지 않는다.
--     (2026_kotlin_migration_contract_prod.sql, 롤백 윈도우 이후 별도 실행)
--
-- 안전장치:
--   - 모든 구조 변경은 information_schema 로 존재 여부를 먼저 확인하므로 재실행해도 안전하다.
--   - 더티 데이터(반쪽 계좌, 소셜계정 중복, 회차 없는 공연 등)가 있으면 SIGNAL 로 즉시
--     중단하고 아무 제약도 걸지 않는다. 데이터는 절대 임의 삭제/합성하지 않는다.
--   - MySQL 은 DDL 이 암묵적 커밋된다. 실행 전 booking / performance / member 백업 권장.
--
-- 실행 (이 파일이 연결된 beatProd 커넥션에서 그대로):
--   mysql -h <prod-db-endpoint> -u <user> -p beatProd < ops/db/migration/2026_kotlin_migration_expand_prod.sql
--
-- 배포 순서(반드시 지킬 것):
--   [1] dev EXPAND(2026_kotlin_migration_expand_dev.sql) 실행, 앱 배포·관측 완료 후
--   → [2] 이 prod EXPAND 실행 → [3] Kotlin 마이그레이션 앱을 prod에 배포
--   → [4] 롤백 윈도우 종료 후 CONTRACT(NOT NULL) 별도 실행
-- =============================================================================


-- =============================================================================
-- STEP 0. 사전 진단 — 모두 0행이면 아래 STEP 1이 중단 없이 통과한다.
--          행이 있으면 먼저 수리하고 재실행한다.
-- =============================================================================
SELECT COUNT(*) AS partial_booking_refund FROM booking
  WHERE NOT ((bank_name IS NULL AND account_number IS NULL AND account_holder IS NULL)
    OR (bank_name IS NOT NULL AND bank_name<>'NONE' AND account_number IS NOT NULL
        AND account_holder IS NOT NULL AND account_number REGEXP '[^[:space:]]'
        AND account_holder REGEXP '[^[:space:]]'));
SELECT COUNT(*) AS perf_no_schedule FROM performance p
  LEFT JOIN schedule s ON s.performance_id=p.id WHERE s.id IS NULL;
SELECT user_id, COUNT(*) FROM member GROUP BY user_id HAVING COUNT(*)>1;
SELECT social_type, social_id, COUNT(*) FROM member GROUP BY social_type, social_id HAVING COUNT(*)>1;


-- =============================================================================
-- STEP 1. EXPAND 프로시저 정의 + 실행
-- =============================================================================
DELIMITER //
DROP PROCEDURE IF EXISTS beat_expand_all//
CREATE PROCEDURE beat_expand_all()
BEGIN
    -- ---- 1. booking.total_payment_amount : 결제금액 스냅샷 컬럼 ----
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'booking'
          AND column_name = 'total_payment_amount'
    ) THEN
        ALTER TABLE booking
            ADD COLUMN total_payment_amount INT NULL AFTER purchase_ticket_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM booking b
        JOIN schedule s ON s.id = b.schedule_id
        JOIN performance p ON p.id = s.performance_id
        WHERE b.purchase_ticket_count < 0
           OR p.ticket_price < 0
           OR CAST(b.purchase_ticket_count AS SIGNED) * CAST(p.ticket_price AS SIGNED) > 2147483647
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Booking payment snapshot exceeds the application Int contract';
    END IF;

    UPDATE booking b
    JOIN schedule s ON s.id = b.schedule_id
    JOIN performance p ON p.id = s.performance_id
    SET b.total_payment_amount = b.purchase_ticket_count * p.ticket_price
    WHERE b.total_payment_amount IS NULL;

    -- ---- 2. booking 환불계좌 all-or-none CHECK ----
    IF EXISTS (
        SELECT 1 FROM booking WHERE NOT (
            (bank_name IS NULL AND account_number IS NULL AND account_holder IS NULL)
            OR (bank_name IS NOT NULL AND bank_name <> 'NONE'
                AND account_number IS NOT NULL AND account_holder IS NOT NULL
                AND account_number REGEXP '[^[:space:]]' AND account_holder REGEXP '[^[:space:]]')
        )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Partial booking refund accounts must be repaired before migration';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'booking'
          AND constraint_name = 'chk_booking_refund_account_complete_v2' AND constraint_type = 'CHECK'
    ) THEN
        ALTER TABLE booking
            ADD CONSTRAINT chk_booking_refund_account_complete_v2 CHECK (
                (bank_name IS NULL AND account_number IS NULL AND account_holder IS NULL)
                OR (bank_name IS NOT NULL AND bank_name <> 'NONE'
                    AND account_number IS NOT NULL AND account_holder IS NOT NULL
                    AND account_number REGEXP '[^[:space:]]' AND account_holder REGEXP '[^[:space:]]')
            );
    END IF;

    -- ---- 3. performance 결제계좌 정규화 + 기간 컬럼 + CHECK 2종 ----
    IF EXISTS (
        SELECT 1 FROM performance WHERE NOT (
            ((bank_name IS NULL OR bank_name = 'NONE')
                AND (account_number IS NULL OR account_number NOT REGEXP '[^[:space:]]')
                AND (account_holder IS NULL OR account_holder NOT REGEXP '[^[:space:]]'))
            OR (bank_name IS NOT NULL AND bank_name <> 'NONE'
                AND account_number IS NOT NULL AND account_holder IS NOT NULL
                AND account_number REGEXP '[^[:space:]]' AND account_holder REGEXP '[^[:space:]]')
        )
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid performance payment accounts must be repaired before migration';
    END IF;

    UPDATE performance
    SET bank_name = NULL, account_number = NULL, account_holder = NULL
    WHERE (bank_name IS NULL OR bank_name = 'NONE')
      AND (account_number IS NULL OR account_number NOT REGEXP '[^[:space:]]')
      AND (account_holder IS NULL OR account_holder NOT REGEXP '[^[:space:]]');

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'performance'
          AND column_name = 'performance_start_date'
    ) THEN
        ALTER TABLE performance ADD COLUMN performance_start_date DATE NULL AFTER performance_period;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'performance'
          AND column_name = 'performance_end_date'
    ) THEN
        ALTER TABLE performance ADD COLUMN performance_end_date DATE NULL AFTER performance_start_date;
    END IF;

    UPDATE performance AS performance
    JOIN (
        SELECT performance_id,
               DATE(MIN(performance_date)) AS start_date,
               DATE(MAX(performance_date)) AS end_date
        FROM schedule GROUP BY performance_id
    ) AS schedule_period ON schedule_period.performance_id = performance.id
    SET performance.performance_start_date = schedule_period.start_date,
        performance.performance_end_date = schedule_period.end_date
    WHERE performance.performance_start_date IS NULL
       OR performance.performance_end_date IS NULL;

    IF EXISTS (
        SELECT 1 FROM performance
        WHERE performance_start_date IS NULL OR performance_end_date IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Every performance needs start and end dates (a performance without any schedule was found)';
    END IF;

    IF EXISTS (
        SELECT 1 FROM performance WHERE performance_start_date > performance_end_date
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Performance start date must not exceed end date';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'performance'
          AND constraint_name = 'chk_performance_payment_account_complete'
    ) THEN
        ALTER TABLE performance
            ADD CONSTRAINT chk_performance_payment_account_complete CHECK (
                (bank_name IS NULL AND account_number IS NULL AND account_holder IS NULL)
                OR (bank_name IS NOT NULL AND bank_name <> 'NONE'
                    AND account_number IS NOT NULL AND account_holder IS NOT NULL
                    AND account_number REGEXP '[^[:space:]]' AND account_holder REGEXP '[^[:space:]]')
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'performance'
          AND constraint_name = 'chk_performance_period_complete'
    ) THEN
        ALTER TABLE performance
            ADD CONSTRAINT chk_performance_period_complete CHECK (
                (performance_start_date IS NULL AND performance_end_date IS NULL)
                OR (performance_start_date IS NOT NULL AND performance_end_date IS NOT NULL
                    AND performance_start_date <= performance_end_date)
            );
    END IF;

    -- ---- 4. member 유일성 제약 ----
    IF EXISTS (SELECT 1 FROM member GROUP BY user_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate member.user_id values must be resolved before migration';
    END IF;

    IF EXISTS (SELECT 1 FROM member GROUP BY social_type, social_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate member social identities must be resolved before migration';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'member'
          AND constraint_name = 'uk_member_user_id' AND constraint_type = 'UNIQUE'
    ) THEN
        ALTER TABLE member ADD CONSTRAINT uk_member_user_id UNIQUE (user_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE() AND table_name = 'member'
          AND constraint_name = 'uk_member_social_identity' AND constraint_type = 'UNIQUE'
    ) THEN
        ALTER TABLE member ADD CONSTRAINT uk_member_social_identity UNIQUE (social_type, social_id);
    END IF;
END//

CALL beat_expand_all()//
DROP PROCEDURE beat_expand_all//
DELIMITER ;


-- =============================================================================
-- STEP 9. 사후 검증 — 아래가 기대값과 일치해야 한다.
-- =============================================================================
SELECT COUNT(*) AS missing_payment_snapshot FROM booking WHERE total_payment_amount IS NULL; -- 0
SELECT column_name, is_nullable FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='performance'
    AND column_name IN ('performance_start_date','performance_end_date');                    -- 2행, YES
SELECT constraint_name FROM information_schema.table_constraints
  WHERE constraint_schema=DATABASE()
    AND constraint_name IN ('chk_booking_refund_account_complete_v2',
       'chk_performance_payment_account_complete','chk_performance_period_complete',
       'uk_member_user_id','uk_member_social_identity');                                     -- 5행
