#!/usr/bin/env bash
set -euo pipefail

resolve_openapi_baselines() {
    local source_repository_root="$1"
    local output_directory="$2"
    local openapi_base_ref="${OPENAPI_BASE_REF:-}"
    local general_baseline
    local admin_baseline
    local baseline_ref
    local general_baseline_exists
    local admin_baseline_exists

    if [[ -n "${openapi_base_ref}" ]]; then
        if ! git -C "${source_repository_root}" cat-file -e "${openapi_base_ref}^{commit}" 2>/dev/null; then
            echo "OpenAPI base git ref '${openapi_base_ref}' does not resolve to a commit" >&2
            return 1
        fi

        general_baseline="${output_directory}/openapi-baseline-general.json"
        admin_baseline="${output_directory}/openapi-baseline-admin.json"

        if git -C "${source_repository_root}" cat-file -e "${openapi_base_ref}:docs/openapi/baseline/general.json" 2>/dev/null; then
            general_baseline_exists=true
        else
            general_baseline_exists=false
        fi
        if git -C "${source_repository_root}" cat-file -e "${openapi_base_ref}:docs/openapi/baseline/admin.json" 2>/dev/null; then
            admin_baseline_exists=true
        else
            admin_baseline_exists=false
        fi

        if [[ "${general_baseline_exists}" == true && "${admin_baseline_exists}" == true ]]; then
            baseline_ref="${openapi_base_ref}"
        elif [[ "${general_baseline_exists}" == false && "${admin_baseline_exists}" == false ]]; then
            baseline_ref="HEAD"
            echo "OpenAPI baselines are absent from '${openapi_base_ref}'; validating the bootstrap baselines committed at HEAD" >&2
        else
            echo "OpenAPI baseline pair is incomplete at git ref '${openapi_base_ref}'" >&2
            return 1
        fi

        if ! git -C "${source_repository_root}" show "${baseline_ref}:docs/openapi/baseline/general.json" > "${general_baseline}"; then
            echo "Failed to read OpenAPI general baseline from git ref '${baseline_ref}'" >&2
            return 1
        fi
        if ! git -C "${source_repository_root}" show "${baseline_ref}:docs/openapi/baseline/admin.json" > "${admin_baseline}"; then
            echo "Failed to read OpenAPI admin baseline from git ref '${baseline_ref}'" >&2
            return 1
        fi
    else
        general_baseline="${source_repository_root}/docs/openapi/baseline/general.json"
        admin_baseline="${source_repository_root}/docs/openapi/baseline/admin.json"
    fi

    printf '%s\n' "${general_baseline}" "${admin_baseline}"
}
