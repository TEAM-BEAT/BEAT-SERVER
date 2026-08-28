#!/usr/bin/env bash

check_compatibility() {
    local oasdiff_binary="$1"
    local baseline="$2"
    local generated="$3"

    [[ -f "${baseline}" ]] || {
        echo "Missing OpenAPI baseline: ${baseline}" >&2
        return 1
    }
    [[ -f "${generated}" ]] || {
        echo "Missing generated OpenAPI document: ${generated}" >&2
        return 1
    }

    "${oasdiff_binary}" breaking "${baseline}" "${generated}" --fail-on ERR
}

assert_canonical_json_equality() {
    local document_name="$1"
    local baseline_path="$2"
    local generated_path="$3"

    if ! cmp --silent \
        <(jq --sort-keys --compact-output . "${baseline_path}") \
        <(jq --sort-keys --compact-output . "${generated_path}"); then
        echo "Committed ${document_name} OpenAPI baseline must canonically match the generated document" >&2
        return 1
    fi
}

run_compatibility_checks() {
    local oasdiff_binary="$1"
    local general_baseline="$2"
    local general_generated="$3"
    local admin_baseline="$4"
    local admin_generated="$5"
    local general_status=0
    local admin_status=0

    if check_compatibility "${oasdiff_binary}" "${general_baseline}" "${general_generated}"; then
        :
    else
        general_status=$?
    fi

    if check_compatibility "${oasdiff_binary}" "${admin_baseline}" "${admin_generated}"; then
        :
    else
        admin_status=$?
    fi

    if (( general_status != 0 || admin_status != 0 )); then
        echo "OpenAPI compatibility failed (general=${general_status}, admin=${admin_status})" >&2
        return 1
    fi
}
