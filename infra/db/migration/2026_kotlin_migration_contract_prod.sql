-- =============================================================================
-- BEAT Kotlin 마이그레이션 CONTRACT — beatProd (지금 실행하지 말 것)
-- MySQL 8.0 / DDL·DML·DCL·TCL 만 사용 (클라이언트 메타 명령 없음)
-- =============================================================================
-- 이 파일은 infra/db/migration/2026_kotlin_migration_contract_dev.sql 과 반드시
-- 동일한 본문(STEP 1)을 유지해야 한다. beatDev/beatProd 는 완전히 분리된 별도 DB
-- 경로이며 이 두 파일을 이어주는 접속 정보나 인클루드 지시어는 존재하지 않는다.
-- 본문을 수정할 때는 두 파일에 반드시 동일하게 반영할 것.
--
-- 실행 시점: EXPAND(2026_kotlin_migration_expand_prod.sql) 배포·안정화 +
--           롤백 윈도우 종료 이후에만 실행한다. 지금 실행하면 이 컬럼을 모르는
--           "구버전 앱"으로 롤백했을 때 INSERT 가 깨져 롤백이 불가능해진다.
--
-- 실행:
--   mysql -h <prod-db-endpoint> -u <user> -p beatProd < infra/db/migration/2026_kotlin_migration_contract_prod.sql
-- =============================================================================


-- =============================================================================
-- STEP 1. CONTRACT 프로시저 정의 + 실행
-- =============================================================================
DELIMITER //
DROP PROCEDURE IF EXISTS beat_contract_all//
CREATE PROCEDURE beat_contract_all()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'performance'
          AND column_name = 'performance_start_date'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'performance'
          AND column_name = 'performance_end_date'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Run EXPAND before CONTRACT';
    END IF;

    IF EXISTS (
        SELECT 1 FROM booking WHERE total_payment_amount IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'booking.total_payment_amount is not contract-ready';
    END IF;

    IF EXISTS (
        SELECT 1 FROM performance
        WHERE performance_start_date IS NULL OR performance_end_date IS NULL
           OR performance_start_date > performance_end_date
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Performance period data is not contract-ready';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'booking'
          AND column_name = 'total_payment_amount' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE booking MODIFY COLUMN total_payment_amount INT NOT NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'performance'
          AND column_name IN ('performance_start_date', 'performance_end_date')
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE performance
            MODIFY COLUMN performance_start_date DATE NOT NULL,
            MODIFY COLUMN performance_end_date DATE NOT NULL;
    END IF;
END//

CALL beat_contract_all()//
DROP PROCEDURE beat_contract_all//
DELIMITER ;

-- Remove the legacy JPA mapping in a follow-up release first.
-- ALTER TABLE performance DROP COLUMN performance_period;

SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name IN ('booking', 'performance')
  AND column_name IN ('total_payment_amount', 'performance_period', 'performance_start_date', 'performance_end_date');
