#!/usr/bin/env bash

set -euo pipefail

# Stock-contention experiment run-control contract
#
# This script only applies the local beatDev schema prerequisite. It does not
# reset fixtures, restart shared infrastructure, or run k6. The following is
# the mandatory operating contract for every measured strategy run.
#
# 1. Shared RDS / InnoDB buffer pool
#    - Read back performance and schedules 16/17 in the same order every time.
#    - Capture these global counters immediately before warmup and after flash:
#        SHOW GLOBAL STATUS WHERE Variable_name IN (
#          'Innodb_buffer_pool_read_requests',
#          'Innodb_buffer_pool_reads',
#          'Innodb_buffer_pool_read_ahead',
#          'Innodb_buffer_pool_read_ahead_evicted',
#          'Innodb_buffer_pool_pages_dirty'
#        );
#    - Treat counter deltas only as shared-RDS context, never as a strategy-
#      attributable result. SELECT * of three fixture rows does not normalize
#      the buffer pool and must not be described as cache pre-touch.
#    - Fixed order: fixture reset/read-back -> quiet >=60s -> pre snapshot ->
#      warmup -> quiet >=60s -> flash -> post snapshot -> cleanup.
#      Keep warmup bookings until flash completes; cleanup between warmup and
#      flash would erase part of the cache/table-state control.
#    - Never restart shared RDS, FLUSH TABLES, drop caches, TRUNCATE, or issue a
#      broad DELETE for this experiment.
#
# 2. Local Mac k6 generator
#    - Record clock offset without changing the system clock:
#        sntp time.apple.com
#      Do not use `sntp -sS`; -s/-S can slew/set the macOS system clock.
#    - Use AC power, the same Mac/network/location, disable sleep, and pause
#      Time Machine/sync/meeting workloads. Prefer wired networking when
#      available. Run the versioned profile without CLI RPS overrides:
#        caffeinate -i k6 run --out opentelemetry stock-contention.js
#    - Record Mac model, macOS version, k6 version, wired/wireless, power,
#      clock offset, runner CPU before/after, dropped iterations, and attempts.
#    - Invalidate and rerun with a new TEST_ID if dropped_iterations >= 1,
#      unexpected responses >= 1, network interruption occurs, or measured
#      generator saturation prevents the configured arrival rate. A generic
#      whole-Mac CPU >=80% alone is context, not proof of generator saturation.
#
# 3. JVM/GC, Hikari, and server page cache
#    - Do not restart the application before every strategy run. That changes
#      the benchmark into a cold-start test and still does not reset the shared
#      RDS buffer pool. Keep one Git SHA/image/JVM/Hikari configuration for a
#      randomized block; reject a run if a restart or deployment occurs.
#    - After the initial deployment, wait for a fixed settle period and capture
#      a >=10-minute baseline. For every run capture GC pause, heap, Hikari
#      active/idle/pending, node/container CPU and memory before and after.
#    - After flash, allow drain, then cooldown for at least 90 seconds and until
#      baseline recovery is visible for consecutive scrape points.
#    - Abort on a Hikari acquire timeout. Invalidate on persistent Hikari
#      pending plus pool saturation 60 seconds after completion, baseline not
#      recovering, deploy/batch interference, or material shared-RDS/prod
#      deviation from a predeclared baseline threshold.
#    - Check BurstBalance only when the RDS storage type is gp2; the AWS metric
#      is specifically the remaining gp2 burst-credit percentage.
#
# A run is not eligible for comparison unless its record contains all three:
# pre/post buffer-pool counters, the Mac generator fields above, and the full
# settle/baseline/warmup/flash/drain/recovery snapshots.

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-13306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-beatDev}"
MYSQL_USER="${MYSQL_USER:-}"

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

[[ -n "$MYSQL_USER" ]] || fail "MYSQL_USER must be set; mysql will prompt for the password"
[[ "$MYSQL_PORT" =~ ^[0-9]+$ ]] || fail "MYSQL_PORT must be numeric"

case "$MYSQL_HOST" in
  *[Pp][Rr][Oo][Dd]*|*[Pp][Rr][Oo][Dd][Uu][Cc][Tt][Ii][Oo][Nn]*)
    fail "production hosts are not allowed"
    ;;
esac

case "$MYSQL_HOST" in
  localhost|127.0.0.1|::1) ;;
  *) fail "this script accepts local MySQL hosts only: localhost, 127.0.0.1, or ::1" ;;
esac

[[ "$MYSQL_DATABASE" == "beatDev" ]] || fail "this script can target only the beatDev schema"
command -v mysql >/dev/null 2>&1 || fail "mysql client is required"

printf 'Applying booking strategy migration to %s:%s/%s\n' \
  "$MYSQL_HOST" "$MYSQL_PORT" "$MYSQL_DATABASE"

mysql \
  --protocol=tcp \
  --host="$MYSQL_HOST" \
  --port="$MYSQL_PORT" \
  --user="$MYSQL_USER" \
  --password \
  --database="$MYSQL_DATABASE" \
  <<'SQL'
DROP PROCEDURE IF EXISTS beat_stock_contention_dev_migration;
DELIMITER //
CREATE PROCEDURE beat_stock_contention_dev_migration()
BEGIN
  DECLARE schedule_table_count INT DEFAULT 0;
  DECLARE version_column_count INT DEFAULT 0;
  DECLARE version_is_compatible INT DEFAULT 0;

  SELECT COUNT(*) INTO schedule_table_count
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'schedule';

  IF schedule_table_count = 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'beatDev must contain the schedule table before this migration';
  END IF;

  SELECT COUNT(*) INTO version_column_count
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'schedule'
    AND column_name = 'version';

  IF version_column_count = 0 THEN
    ALTER TABLE schedule
      ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER sold_ticket_count;
  ELSE
    SELECT COUNT(*) INTO version_is_compatible
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'schedule'
      AND column_name = 'version'
      AND data_type = 'bigint'
      AND is_nullable = 'NO'
      AND (column_default = '0' OR column_default = 0);

    IF version_is_compatible = 0 THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'schedule.version exists with an incompatible definition';
    END IF;
  END IF;
END//
DELIMITER ;
CALL beat_stock_contention_dev_migration();
DROP PROCEDURE beat_stock_contention_dev_migration;
SQL

printf 'Applied schedule.version to %s\n' "$MYSQL_DATABASE"
