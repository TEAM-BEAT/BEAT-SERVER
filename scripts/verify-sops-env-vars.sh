#!/usr/bin/env bash
# =============================================================================
# BEAT-SERVER SOPS 환경변수 검증 스크립트
# =============================================================================
# 코드에서 참조하는 환경변수가 SOPS secret에 모두 포함되어 있는지 검증한다.
#
# 사용법:
#   ./scripts/verify-sops-env-vars.sh dev   # dev 환경 검증
#   ./scripts/verify-sops-env-vars.sh prod  # prod 환경 검증
#
# 전제조건:
#   - sops + age 설치 (brew install sops age)
#   - age private key 존재 (~/.config/sops/age/keys.txt 또는 SOPS_AGE_KEY_FILE)
# =============================================================================

set -euo pipefail

PROFILE="${1:-}"
if [[ -z "$PROFILE" ]] || [[ "$PROFILE" != "dev" && "$PROFILE" != "prod" ]]; then
    echo "Usage: $0 <dev|prod>"
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOPS_FILE="$REPO_ROOT/ops/ansible/inventories/$PROFILE/group_vars/all/secrets.sops.yml"
MAIN_VARS="$REPO_ROOT/ops/ansible/inventories/$PROFILE/group_vars/all/main.yml"
PREFIX="$(echo "$PROFILE" | tr '[:lower:]' '[:upper:]')"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  BEAT-SERVER SOPS 환경변수 검증 — $PROFILE 환경"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ---- 1. 코드에서 참조하는 환경변수 추출 ----
echo ""
echo "📋 Step 1: 코드에서 ${PREFIX}_* 및 공통 환경변수 참조 추출"

CODE_VARS=$(grep -roh "\${${PREFIX}_[A-Z0-9_]*}" \
    "$REPO_ROOT/apps" \
    "$REPO_ROOT/infrastructure/src/main/resources" \
    "$REPO_ROOT/support" \
    --include='*.yml' --include='*.yaml' --include='*.properties' \
    2>/dev/null \
    | sed -e 's/^\${//' -e 's/}$//' \
    | sort -u)

# 공통 변수도 추가
SHARED_VARS=$(grep -roh '\${\(DB_HIKARI_MAX_POOL_SIZE\|SENTRY_DSN\|SENTRY_ENVIRONMENT\|SENTRY_RELEASE\|BEAT_LOG_FORMAT\)}' \
    "$REPO_ROOT/apps" \
    "$REPO_ROOT/infrastructure/src/main/resources" \
    "$REPO_ROOT/support" \
    --include='*.yml' --include='*.yaml' --include='*.properties' \
    2>/dev/null \
    | sed -e 's/^\${//' -e 's/}$//' \
    | sort -u || true)

ALL_CODE_VARS=$(echo -e "$CODE_VARS\n$SHARED_VARS" | sort -u | grep -v '^$')
CODE_COUNT=$(echo "$ALL_CODE_VARS" | wc -l | tr -d ' ')
echo "   → 코드 참조 변수 ${CODE_COUNT}개 발견"

# ---- 2. SOPS 복호화하여 시크릿 변수 추출 ----
echo ""
echo "🔐 Step 2: SOPS 복호화 → app_secret_content 변수명 추출"

if ! command -v sops &> /dev/null; then
    echo "   ❌ sops 미설치. brew install sops age 후 재실행"
    exit 1
fi

# app_secret_content 추출 (KEY=VALUE 형식에서 KEY만)
SOPS_CONTENT=$(sops -d --extract '["app_secret_content"]' "$SOPS_FILE" 2>/dev/null) || {
    echo "   ❌ SOPS 복호화 실패. age private key 확인 필요"
    echo "      SOPS_AGE_KEY_FILE: ${SOPS_AGE_KEY_FILE:-~/.config/sops/age/keys.txt}"
    exit 1
}

SECRET_VARS=$(echo "$SOPS_CONTENT" \
    | grep -v '^#' \
    | grep -v '^$' \
    | sed 's/=.*//' \
    | sort -u)

# actuator 변수는 Ansible 템플릿이 별도 주입 (secrets.sops.yml 직접)
ACTUATOR_PORT=$(sops -d --extract '["actuator_port"]' "$SOPS_FILE" 2>/dev/null && echo "${PREFIX}_ACTUATOR_PORT" || true)
ACTUATOR_PATH=$(sops -d --extract '["actuator_path"]' "$SOPS_FILE" 2>/dev/null && echo "${PREFIX}_ACTUATOR_PATH" || true)

# Ansible main.yml에서 hikari pool size 확인
HIKARI_IN_MAIN=$(grep -c 'hikari_max_pool_size' "$MAIN_VARS" 2>/dev/null || echo "0")

ALL_SECRET_VARS=$(echo -e "$SECRET_VARS\n${PREFIX}_ACTUATOR_PORT\n${PREFIX}_ACTUATOR_PATH" | sort -u | grep -v '^$')
SECRET_COUNT=$(echo "$ALL_SECRET_VARS" | wc -l | tr -d ' ')
echo "   → SOPS 시크릿 변수 ${SECRET_COUNT}개 발견"

# ---- 3. 차이 비교 ----
echo ""
echo "🔍 Step 3: 누락 변수 검사"

MISSING=0
while IFS= read -r var; do
    [[ -z "$var" ]] && continue

    # DB_HIKARI_MAX_POOL_SIZE는 Ansible main.yml에서 주입
    if [[ "$var" == "DB_HIKARI_MAX_POOL_SIZE" ]]; then
        if [[ "$HIKARI_IN_MAIN" -gt 0 ]]; then
            continue
        fi
    fi

    # SENTRY_RELEASE, BEAT_LOG_FORMAT은 컨테이너 런타임 주입
    if [[ "$var" == "SENTRY_RELEASE" || "$var" == "BEAT_LOG_FORMAT" || "$var" == "SENTRY_ENVIRONMENT" ]]; then
        continue
    fi

    if ! echo "$ALL_SECRET_VARS" | grep -qx "$var"; then
        echo "   ⚠️  누락: $var (코드에서 참조하지만 SOPS에 없음)"
        MISSING=$((MISSING + 1))
    fi
done <<< "$ALL_CODE_VARS"

# 역방향: SOPS에만 있고 코드에서 안 쓰는 변수
echo ""
echo "🔍 Step 4: 미사용 시크릿 검사 (SOPS에만 존재)"

UNUSED=0
while IFS= read -r var; do
    [[ -z "$var" ]] && continue
    if ! echo "$ALL_CODE_VARS" | grep -qx "$var"; then
        echo "   ℹ️  미사용: $var (SOPS에 있지만 코드에서 참조 안 함)"
        UNUSED=$((UNUSED + 1))
    fi
done <<< "$ALL_SECRET_VARS"

# ---- 4. 결과 요약 ----
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  검증 결과 ($PROFILE)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  코드 참조 변수:    ${CODE_COUNT}개"
echo "  SOPS 시크릿 변수:  ${SECRET_COUNT}개"
echo "  ⚠️  누락 (코드 → SOPS): ${MISSING}개"
echo "  ℹ️  미사용 (SOPS → 코드): ${UNUSED}개"
echo ""

if [[ $MISSING -gt 0 ]]; then
    echo "  ❌ 누락 변수가 있습니다. SOPS에 추가 필요!"
    exit 1
else
    echo "  ✅ 모든 코드 참조 변수가 SOPS에 존재합니다."
    exit 0
fi
