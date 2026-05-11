#!/bin/sh

set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
TMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/beat-generate-local-dev-secret-test.XXXXXX")
cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT HUP INT TERM

FAKE_SOPS="$TMP_DIR/sops"
cat > "$FAKE_SOPS" <<'EOS'
#!/bin/sh
set -eu

if [ "$1" != "-d" ] || [ "$2" != "--extract" ]; then
  echo "unexpected sops invocation: $*" >&2
  exit 64
fi

case "$3" in
  '["app_secret_content"]')
    cat <<'PROPS'
DEV_DB_URL=jdbc:mysql://mysql:3306/exampleDb
DEV_REDIS_HOST=redis
DEV_ACTUATOR_PORT=1111
DEV_ACTUATOR_PATH=/old-actuator
DEV_UNRELATED=value
PROPS
    ;;
  '["actuator_port"]')
    printf '%s\n' '18080'
    ;;
  '["actuator_path"]')
    printf '%s\n' '/new-actuator'
    ;;
  *)
    echo "unexpected extract: $3" >&2
    exit 65
    ;;
esac
EOS
chmod +x "$FAKE_SOPS"

touch "$TMP_DIR/secrets.sops.yml"

assert_line() {
  file="$1"
  expected="$2"
  if ! grep -qx "$expected" "$file"; then
    echo "missing expected line: $expected" >&2
    echo "--- $file ---" >&2
    cat "$file" >&2
    exit 1
  fi
}

run_generator() {
  output_file="$1"
  shift
  env \
    PATH="$TMP_DIR:$PATH" \
    SOPS_AGE_KEY='AGE-SECRET-KEY-test' \
    SOPS_FILE="$TMP_DIR/secrets.sops.yml" \
    OUTPUT_FILE="$output_file" \
    "$@" \
    "$ROOT_DIR/scripts/generate-local-dev-secret.sh" >/dev/null
}

DEFAULT_OUTPUT="$TMP_DIR/default.properties"
run_generator "$DEFAULT_OUTPUT"
assert_line "$DEFAULT_OUTPUT" 'DEV_DB_URL=jdbc:mysql://localhost:13306/exampleDb'
assert_line "$DEFAULT_OUTPUT" 'DEV_REDIS_HOST=localhost'
assert_line "$DEFAULT_OUTPUT" 'DEV_ACTUATOR_PORT=18080'
assert_line "$DEFAULT_OUTPUT" 'DEV_ACTUATOR_PATH=/new-actuator'
assert_line "$DEFAULT_OUTPUT" 'DEV_UNRELATED=value'

printf '%s\n' 'generate-local-dev-secret tests passed'
