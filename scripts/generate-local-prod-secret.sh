#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOPS_FILE="${SOPS_FILE:-$ROOT_DIR/infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml}"
OUTPUT_FILE="${OUTPUT_FILE:-$ROOT_DIR/secret/application-prod-secret.properties}"

. "$ROOT_DIR/scripts/lib/local-vars.sh"

require_sops_identity

if ! command -v sops >/dev/null 2>&1; then
  echo "sops is required to generate the local prod secret file" >&2
  exit 1
fi

if [ ! -f "$SOPS_FILE" ]; then
  echo "Missing inventory file: $SOPS_FILE" >&2
  echo "Provide a real encrypted prod secret vars file at infra/ansible/inventories/prod/group_vars/all/secrets.sops.yml or override SOPS_FILE." >&2
  exit 1
fi

TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/beat-local-prod-secret.XXXXXX")
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT HUP INT TERM

mkdir -p "$(dirname -- "$OUTPUT_FILE")"

sops -d --extract '["app_secret_content"]' "$SOPS_FILE" > "$TMP_DIR/app_secret_content.properties"
ACTUATOR_PORT=$(sops -d --extract '["actuator_port"]' "$SOPS_FILE")
ACTUATOR_PATH=$(sops -d --extract '["actuator_path"]' "$SOPS_FILE")

awk '
  $0 !~ "^PROD_ACTUATOR_PORT=" &&
  $0 !~ "^PROD_ACTUATOR_PATH="
' "$TMP_DIR/app_secret_content.properties" > "$TMP_DIR/app_secret_content.filtered.properties"

{
  cat "$TMP_DIR/app_secret_content.filtered.properties"
  printf '\nPROD_ACTUATOR_PORT=%s\n' "$ACTUATOR_PORT"
  printf 'PROD_ACTUATOR_PATH=%s\n' "$ACTUATOR_PATH"
} > "$TMP_DIR/application-prod-secret.properties"

mv "$TMP_DIR/application-prod-secret.properties" "$OUTPUT_FILE"
chmod 400 "$OUTPUT_FILE"

printf 'Generated %s from %s\n' "$OUTPUT_FILE" "$SOPS_FILE"

