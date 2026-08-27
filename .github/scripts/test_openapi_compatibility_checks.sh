#!/usr/bin/env bash
set -euo pipefail

script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly script_directory
test_directory="$(mktemp -d)"
readonly test_directory
trap 'rm -rf -- "${test_directory}"' EXIT

# shellcheck source=.github/scripts/openapi_compatibility_checks.sh
source "${script_directory}/openapi_compatibility_checks.sh"

readonly fake_oasdiff="${test_directory}/oasdiff"
readonly invocation_log="${test_directory}/invocations"
cat > "${fake_oasdiff}" <<'FAKE_OASDIFF'
#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "${2##*/}" >> "${INVOCATION_LOG}"
case "$2" in
    *general-failure) exit "${GENERAL_STATUS}" ;;
    *admin-failure) exit "${ADMIN_STATUS}" ;;
esac
FAKE_OASDIFF
chmod +x "${fake_oasdiff}"

run_case() {
    local expected_status="$1"
    local expected_invocations="$2"
    local expected_message="$3"
    local general_status="$4"
    local admin_status="$5"
    local output
    local actual_status=0

    : > "${invocation_log}"
    if output="$({
        GENERAL_STATUS="${general_status}" \
        ADMIN_STATUS="${admin_status}" \
        INVOCATION_LOG="${invocation_log}" \
        run_compatibility_checks \
            "${fake_oasdiff}" \
            "${test_directory}/general-failure" \
            "${test_directory}/general-generated.json" \
            "${test_directory}/admin-failure" \
            "${test_directory}/admin-generated.json"
    } 2>&1)"; then
        actual_status=0
    else
        actual_status=$?
    fi

    [[ "${actual_status}" -eq "${expected_status}" ]] || {
        echo "Expected status ${expected_status}, got ${actual_status}: ${output}" >&2
        exit 1
    }
    if [[ -n "${expected_message}" && "${output}" != *"${expected_message}"* ]]; then
        echo "Expected output to contain '${expected_message}': ${output}" >&2
        exit 1
    fi
    [[ "$(<"${invocation_log}")" == "${expected_invocations}" ]] || {
        echo "Unexpected oasdiff invocations: $(<"${invocation_log}")" >&2
        exit 1
    }
}

: > "${test_directory}/general-failure"
: > "${test_directory}/admin-failure"
: > "${test_directory}/general-generated.json"
: > "${test_directory}/admin-generated.json"

run_case 0 $'general-failure\nadmin-failure' '' 0 0
run_case 1 $'general-failure\nadmin-failure' 'general=1, admin=0' 1 0
run_case 1 $'general-failure\nadmin-failure' 'general=1, admin=1' 1 1
run_case 1 $'general-failure\nadmin-failure' 'general=0, admin=1' 0 1
run_case 1 $'general-failure\nadmin-failure' 'general=128, admin=128' 128 128

echo "OpenAPI compatibility checks test passed"
