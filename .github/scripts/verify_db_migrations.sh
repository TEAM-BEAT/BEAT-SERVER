#!/usr/bin/env bash
set -euo pipefail

readonly mysql_image="${MYSQL_IMAGE:-mysql:8.4.11}"
readonly database_name="beat_migration_test"
readonly mysql_root_password="beat_migration_test_root"
readonly container_name="beat-migration-test-${BASHPID}-${RANDOM}"
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly repository_root
readonly expand_dev_file="${repository_root}/ops/db/migration/2026_kotlin_migration_expand_dev.sql"
readonly expand_prod_file="${repository_root}/ops/db/migration/2026_kotlin_migration_expand_prod.sql"
readonly contract_dev_file="${repository_root}/ops/db/migration/2026_kotlin_migration_contract_dev.sql"
readonly contract_prod_file="${repository_root}/ops/db/migration/2026_kotlin_migration_contract_prod.sql"

command -v docker >/dev/null 2>&1 || {
    echo "Docker CLI is required to verify database migrations" >&2
    exit 1
}

migration_body() {
    local migration_file="$1"

    awk '
        /^DELIMITER \/\/$/ { found = 1 }
        found { print }
    ' "${migration_file}"
}

compare_migration_bodies() {
    local dev_file="$1"
    local prod_file="$2"
    local migration_name="$3"
    local dev_body
    local prod_body

    [[ -f "${dev_file}" ]] || {
        echo "Missing ${migration_name} dev migration: ${dev_file}" >&2
        return 1
    }
    [[ -f "${prod_file}" ]] || {
        echo "Missing ${migration_name} prod migration: ${prod_file}" >&2
        return 1
    }

    dev_body="$(migration_body "${dev_file}")"
    prod_body="$(migration_body "${prod_file}")"
    [[ -n "${dev_body}" && -n "${prod_body}" ]] || {
        echo "Could not find the first DELIMITER // in ${migration_name} migrations" >&2
        return 1
    }
    [[ "${dev_body}" == "${prod_body}" ]] || {
        echo "${migration_name} dev/prod executable bodies differ" >&2
        return 1
    }
}

compare_migration_bodies "${expand_dev_file}" "${expand_prod_file}" "expand"
compare_migration_bodies "${contract_dev_file}" "${contract_prod_file}" "contract"

cleanup() {
    docker rm --force "${container_name}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run --detach \
    --name "${container_name}" \
    --env "MYSQL_ROOT_PASSWORD=${mysql_root_password}" \
    "${mysql_image}" >/dev/null

wait_for_mysql() {
    local attempt

    for ((attempt = 1; attempt <= 60; attempt++)); do
        if docker exec "${container_name}" mysqladmin ping \
            --host=127.0.0.1 \
            --user=root \
            --password="${mysql_root_password}" \
            --silent >/dev/null 2>&1; then
            return 0
        fi
        sleep 1
    done

    echo "MySQL did not become ready within 60 seconds" >&2
    docker logs "${container_name}" >&2 || true
    return 1
}

wait_for_mysql

mysql_command() {
    docker exec -i "${container_name}" mysql \
        --protocol=socket \
        --user=root \
        --password="${mysql_root_password}" \
        "$@"
}

mysql_command <<'SQL'
CREATE DATABASE beat_migration_test;
USE beat_migration_test;

CREATE TABLE performance (
    id BIGINT NOT NULL,
    ticket_price INT NOT NULL,
    bank_name VARCHAR(100) NULL,
    account_number VARCHAR(100) NULL,
    account_holder VARCHAR(100) NULL,
    performance_period VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE schedule (
    id BIGINT NOT NULL,
    performance_id BIGINT NOT NULL,
    performance_date DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE booking (
    id BIGINT NOT NULL,
    purchase_ticket_count INT NOT NULL,
    schedule_id BIGINT NOT NULL,
    bank_name VARCHAR(100) NULL,
    account_number VARCHAR(100) NULL,
    account_holder VARCHAR(100) NULL,
    PRIMARY KEY (id)
);

CREATE TABLE member (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    social_type VARCHAR(64) NOT NULL,
    social_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO performance (
    id, ticket_price, bank_name, account_number, account_holder, performance_period
) VALUES (1, 12500, NULL, NULL, NULL, 'legacy-period');
INSERT INTO schedule (id, performance_id, performance_date)
VALUES (1, 1, '2026-08-01 19:00:00');
INSERT INTO booking (
    id, purchase_ticket_count, schedule_id, bank_name, account_number, account_holder
) VALUES (1, 2, 1, NULL, NULL, NULL);
INSERT INTO member (id, user_id, social_type, social_id)
VALUES (1, 1001, 'KAKAO', 'migration-test-social-id');
SQL

run_migration() {
    local migration_file="$1"

    mysql_command "${database_name}" < "${migration_file}"
}

run_migration "${expand_dev_file}"
run_migration "${expand_dev_file}"

query() {
    local sql="$1"

    mysql_command --batch --skip-column-names --raw --execute="${sql}" "${database_name}"
}

assert_query() {
    local description="$1"
    local expected="$2"
    local sql="$3"
    local actual

    actual="$(query "${sql}")"
    if [[ "${actual}" != "${expected}" ]]; then
        echo "${description}: expected '${expected}', got '${actual}'" >&2
        return 1
    fi
}

assert_query \
    "booking payment snapshot" \
    "25000" \
    "SELECT total_payment_amount FROM booking WHERE id = 1;"
assert_query \
    "performance period dates" \
    $'2026-08-01\t2026-08-01' \
    "SELECT DATE_FORMAT(performance_start_date, '%Y-%m-%d'), DATE_FORMAT(performance_end_date, '%Y-%m-%d') FROM performance WHERE id = 1;"
assert_query \
    "migration constraint count" \
    "5" \
    "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND ((table_name = 'booking' AND constraint_name = 'chk_booking_refund_account_complete_v2') OR (table_name = 'performance' AND constraint_name IN ('chk_performance_payment_account_complete', 'chk_performance_period_complete')) OR (table_name = 'member' AND constraint_name IN ('uk_member_user_id', 'uk_member_social_identity')));"

assert_constraint() {
    local table_name="$1"
    local constraint_name="$2"
    local constraint_type="$3"

    assert_query \
        "${table_name}.${constraint_name}" \
        "${constraint_type}" \
        "SELECT constraint_type FROM information_schema.table_constraints WHERE constraint_schema = DATABASE() AND table_name = '${table_name}' AND constraint_name = '${constraint_name}';"
}

assert_constraint "booking" "chk_booking_refund_account_complete_v2" "CHECK"
assert_constraint "performance" "chk_performance_payment_account_complete" "CHECK"
assert_constraint "performance" "chk_performance_period_complete" "CHECK"
assert_constraint "member" "uk_member_user_id" "UNIQUE"
assert_constraint "member" "uk_member_social_identity" "UNIQUE"

assert_query \
    "expand nullability" \
    $'booking\ttotal_payment_amount\tYES\nperformance\tperformance_end_date\tYES\nperformance\tperformance_start_date\tYES' \
    "SELECT table_name, column_name, is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() AND ((table_name = 'booking' AND column_name = 'total_payment_amount') OR (table_name = 'performance' AND column_name IN ('performance_start_date', 'performance_end_date'))) ORDER BY table_name, column_name;"

run_migration "${contract_dev_file}"
run_migration "${contract_dev_file}"

assert_query \
    "contract nullability" \
    $'booking\ttotal_payment_amount\tNO\nperformance\tperformance_end_date\tNO\nperformance\tperformance_start_date\tNO' \
    "SELECT table_name, column_name, is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() AND ((table_name = 'booking' AND column_name = 'total_payment_amount') OR (table_name = 'performance' AND column_name IN ('performance_start_date', 'performance_end_date'))) ORDER BY table_name, column_name;"

echo "Database migration verification passed for ${mysql_image}"
