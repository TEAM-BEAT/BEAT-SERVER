#!/usr/bin/env bash
set -euo pipefail

resolve_openapi_baselines() {
    local source_repository_root="$1"
    local output_directory="$2"
    local openapi_base_ref="${OPENAPI_BASE_REF:-}"
    local general_baseline
    local admin_baseline

    if [[ -n "${openapi_base_ref}" ]]; then
        general_baseline="${output_directory}/openapi-baseline-general.json"
        admin_baseline="${output_directory}/openapi-baseline-admin.json"

        if ! git -C "${source_repository_root}" show "${openapi_base_ref}:docs/openapi/baseline/general.json" > "${general_baseline}"; then
            echo "Failed to read OpenAPI general baseline from git ref '${openapi_base_ref}'" >&2
            return 1
        fi
        if ! git -C "${source_repository_root}" show "${openapi_base_ref}:docs/openapi/baseline/admin.json" > "${admin_baseline}"; then
            echo "Failed to read OpenAPI admin baseline from git ref '${openapi_base_ref}'" >&2
            return 1
        fi
    else
        general_baseline="${source_repository_root}/docs/openapi/baseline/general.json"
        admin_baseline="${source_repository_root}/docs/openapi/baseline/admin.json"
    fi

    printf '%s\n' "${general_baseline}" "${admin_baseline}"
}
