#!/usr/bin/env bash
set -euo pipefail

readonly oasdiff_version="v1.28.0"
platform_name="$(uname -s)"
readonly platform_name
platform_architecture="$(uname -m)"
readonly platform_architecture

case "${platform_name}:${platform_architecture}" in
    Linux:x86_64|Linux:amd64)
        oasdiff_asset="oasdiff_1.28.0_linux_amd64.tar.gz"
        oasdiff_sha256="e0ef076f2cf953d922addc04be9c3851cf3ec18f7678d2b94d44cea23dca51b5"
        ;;
    Darwin:arm64|Darwin:x86_64)
        oasdiff_asset="oasdiff_1.28.0_darwin_all.tar.gz"
        oasdiff_sha256="ff76474bf47bfb806d1711aa3e962b8e55570badcd462fa487b80aa532a823db"
        ;;
    *)
        echo "Unsupported platform: ${platform_name} ${platform_architecture}" >&2
        exit 1
        ;;
esac

readonly oasdiff_url="https://github.com/oasdiff/oasdiff/releases/download/${oasdiff_version}/${oasdiff_asset}"
readonly oasdiff_sha256
script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly script_directory
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly repository_root
temporary_directory="$(mktemp -d)"
readonly temporary_directory
readonly archive_path="${temporary_directory}/oasdiff.tar.gz"
readonly oasdiff_path="${temporary_directory}/oasdiff"
# shellcheck source=.github/scripts/resolve_openapi_baselines.sh
source "${script_directory}/resolve_openapi_baselines.sh"
# shellcheck source=.github/scripts/openapi_compatibility_checks.sh
source "${script_directory}/openapi_compatibility_checks.sh"

trap 'rm -rf -- "${temporary_directory}"' EXIT

curl --fail --silent --show-error --location --output "${archive_path}" "${oasdiff_url}"
if command -v sha256sum >/dev/null 2>&1; then
    printf '%s  %s\n' "${oasdiff_sha256}" "${archive_path}" | sha256sum -c -
elif command -v shasum >/dev/null 2>&1; then
    printf '%s  %s\n' "${oasdiff_sha256}" "${archive_path}" | shasum -a 256 -c -
else
    echo "Neither sha256sum nor shasum is available for checksum verification" >&2
    exit 1
fi
tar --extract --gzip --file "${archive_path}" --directory "${temporary_directory}"

[[ -x "${oasdiff_path}" ]] || {
    echo "oasdiff binary was not found after extraction" >&2
    exit 1
}

readonly baseline_paths_file="${temporary_directory}/openapi-baseline-paths"
resolve_openapi_baselines "${repository_root}" "${temporary_directory}" > "${baseline_paths_file}"
baseline_paths=()
while IFS= read -r baseline_path; do baseline_paths+=("${baseline_path}"); done < "${baseline_paths_file}"
[[ "${#baseline_paths[@]}" -eq 2 ]] || {
    echo "Expected general and admin OpenAPI baselines" >&2
    exit 1
}

run_compatibility_checks \
    "${oasdiff_path}" \
    "${baseline_paths[0]}" \
    "${repository_root}/apps/api/build/openapi/general.json" \
    "${baseline_paths[1]}" \
    "${repository_root}/apps/admin/build/openapi/admin.json" || exit 1
