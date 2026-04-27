#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOPS_FILE="${SOPS_FILE:-$ROOT_DIR/infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml}"
OUTPUT_FILE="${OUTPUT_FILE:-$ROOT_DIR/secret/application-dev-secret.properties}"

. "$ROOT_DIR/scripts/lib/local-vars.sh"
  
require_sops_identity

if ! command -v sops >/dev/null 2>&1; then
  echo "sops is required to generate the local dev secret file" >&2
  exit 1
fi

if [ ! -f "$SOPS_FILE" ]; then
  echo "Missing inventory file: $SOPS_FILE" >&2
  echo "Provide a real encrypted dev secret vars file at infra/ansible/inventories/dev/group_vars/all/secrets.sops.yml or override SOPS_FILE." >&2
  exit 1
fi

TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/beat-local-dev-secret.XXXXXX")
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT HUP INT TERM

mkdir -p "$(dirname -- "$OUTPUT_FILE")"

extract_property() {
  property_name="$1"
  awk -F= -v property_name="$property_name" '$1 == property_name {
    print substr($0, index($0, "=") + 1)
    exit
  }' "$TMP_DIR/app_secret_content.properties"
}

replace_jdbc_mysql_host() {
  jdbc_url="$1"
  replacement_host="$2"
  prefix="jdbc:mysql://"
  case "$jdbc_url" in
    "$prefix"*) ;;
    *)
      printf '%s\n' "$jdbc_url"
      return
      ;;
  esac

  remainder=${jdbc_url#"$prefix"}
  case "$remainder" in
    */*)
      authority=${remainder%%/*}
      path=/${remainder#*/}
      ;;
    *)
      printf '%s\n' "$jdbc_url"
      return
      ;;
  esac

  port=""
  case "$authority" in
    \[*\]*)
      after_bracket=${authority#*]}
      case "$after_bracket" in
        :*) port="$after_bracket" ;;
      esac
      ;;
    *:*) port=:${authority##*:} ;;
  esac

  printf '%s%s%s%s\n' "$prefix" "$replacement_host" "$port" "$path"
}

sops -d --extract '["app_secret_content"]' "$SOPS_FILE" > "$TMP_DIR/app_secret_content.properties"
ACTUATOR_PORT=$(sops -d --extract '["actuator_port"]' "$SOPS_FILE")
ACTUATOR_PATH=$(sops -d --extract '["actuator_path"]' "$SOPS_FILE")
DEV_DB_URL=$(extract_property "DEV_DB_URL")
if [ -z "$DEV_DB_URL" ]; then
  echo "DEV_DB_URL is missing from app_secret_content" >&2
  exit 1
fi
LOCAL_DEV_DB_URL=$(replace_jdbc_mysql_host "$DEV_DB_URL" "localhost")

awk '
  $0 !~ "^DEV_DB_URL=" &&
  $0 !~ "^DEV_REDIS_HOST=" &&
  $0 !~ "^DEV_ACTUATOR_PORT=" &&
  $0 !~ "^DEV_ACTUATOR_PATH="
' "$TMP_DIR/app_secret_content.properties" > "$TMP_DIR/app_secret_content.filtered.properties"

{
  cat "$TMP_DIR/app_secret_content.filtered.properties"
  printf '\nDEV_DB_URL=%s\n' "$LOCAL_DEV_DB_URL"
  printf 'DEV_ACTUATOR_PORT=%s\n' "$ACTUATOR_PORT"
  printf 'DEV_ACTUATOR_PATH=%s\n' "$ACTUATOR_PATH"
  printf 'DEV_REDIS_HOST=%s\n' "localhost"
} > "$TMP_DIR/application-dev-secret.properties"

mv "$TMP_DIR/application-dev-secret.properties" "$OUTPUT_FILE"
chmod 400 "$OUTPUT_FILE"

printf 'Generated %s from %s\n' "$OUTPUT_FILE" "$SOPS_FILE"
