#!/usr/bin/env bash
set -euo pipefail

script_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly script_directory
unset OPENAPI_BREAKING_APPROVED
test_directory="$(mktemp -d)"
readonly test_directory
readonly git_repository="${test_directory}/repository"
readonly temporary_directory="${test_directory}/temporary"
trap 'rm -rf -- "${test_directory}"' EXIT

mkdir -p "${git_repository}/docs/openapi/baseline" "${temporary_directory}"
git -C "${git_repository}" -c init.defaultBranch=main init --quiet
git -C "${git_repository}" config user.email "ci@example.com"
git -C "${git_repository}" config user.name "CI"
printf '%s\n' '{"source":"committed-general"}' > "${git_repository}/docs/openapi/baseline/general.json"
printf '%s\n' '{"source":"committed-admin"}' > "${git_repository}/docs/openapi/baseline/admin.json"
git -C "${git_repository}" add docs/openapi/baseline
git -C "${git_repository}" commit --quiet -m "baseline"
base_ref="$(git -C "${git_repository}" rev-parse HEAD)"
readonly base_ref

printf '%s\n' '{"source":"working-tree-general"}' > "${git_repository}/docs/openapi/baseline/general.json"
printf '%s\n' '{"source":"working-tree-admin"}' > "${git_repository}/docs/openapi/baseline/admin.json"

# shellcheck source=.github/scripts/resolve_openapi_baselines.sh
source "${script_directory}/resolve_openapi_baselines.sh"
export OPENAPI_BASE_REF="${base_ref}"
readonly resolved_paths_file="${temporary_directory}/resolved-paths"
readonly resolution_log="${temporary_directory}/resolution.log"
resolve_openapi_baselines "${git_repository}" "${temporary_directory}" > "${resolved_paths_file}" 2> "${resolution_log}"
resolved_paths=()
while IFS= read -r resolved_path; do resolved_paths+=("${resolved_path}"); done < "${resolved_paths_file}"
[[ "${#resolved_paths[@]}" -eq 2 ]] || {
    echo "Expected general and admin baseline paths" >&2
    exit 1
}
[[ "$(<"${resolution_log}")" == "OpenAPI baselines at HEAD match '${base_ref}'; validating the baselines from the base ref" ]] || {
    echo "OPENAPI_BASE_REF did not report that the committed HEAD baselines match the base ref" >&2
    exit 1
}

[[ "$(<"${resolved_paths[0]}")" == '{"source":"committed-general"}' ]] || {
    echo "OPENAPI_BASE_REF did not resolve the committed general baseline" >&2
    exit 1
}
[[ "$(<"${resolved_paths[1]}")" == '{"source":"committed-admin"}' ]] || {
    echo "OPENAPI_BASE_REF did not resolve the committed admin baseline" >&2
    exit 1
}
[[ "$(<"${git_repository}/docs/openapi/baseline/general.json")" == '{"source":"working-tree-general"}' ]] || {
    echo "The general working-tree baseline fixture was not changed" >&2
    exit 1
}
[[ "$(<"${git_repository}/docs/openapi/baseline/admin.json")" == '{"source":"working-tree-admin"}' ]] || {
    echo "The admin working-tree baseline fixture was not changed" >&2
    exit 1
}

printf '%s\n' '{"source":"head-general"}' > "${git_repository}/docs/openapi/baseline/general.json"
printf '%s\n' '{"source":"committed-admin"}' > "${git_repository}/docs/openapi/baseline/admin.json"
git -C "${git_repository}" add docs/openapi/baseline
git -C "${git_repository}" commit --quiet -m "update general baseline"
printf '%s\n' '{"source":"working-tree-after-head-general"}' > "${git_repository}/docs/openapi/baseline/general.json"
printf '%s\n' '{"source":"working-tree-after-head-admin"}' > "${git_repository}/docs/openapi/baseline/admin.json"

head_paths_file="${temporary_directory}/head-paths"
resolve_openapi_baselines "${git_repository}" "${temporary_directory}" > "${head_paths_file}" 2> "${resolution_log}"
head_paths=()
while IFS= read -r head_path; do head_paths+=("${head_path}"); done < "${head_paths_file}"
[[ "${#head_paths[@]}" -eq 2 ]] || {
    echo "Expected general and admin HEAD baseline paths" >&2
    exit 1
}
[[ "$(<"${resolution_log}")" == "OpenAPI baselines at HEAD differ from '${base_ref}'; approval label is absent, validating the baselines from the base ref" ]] || {
    echo "OPENAPI_BASE_REF did not preserve the base baselines without approval" >&2
    exit 1
}
[[ "$(<"${head_paths[0]}")" == '{"source":"committed-general"}' ]] || {
    echo "OPENAPI_BASE_REF did not select the base general baseline without approval" >&2
    exit 1
}
[[ "$(<"${head_paths[1]}")" == '{"source":"committed-admin"}' ]] || {
    echo "OPENAPI_BASE_REF did not select the base admin baseline without approval" >&2
    exit 1
}
[[ "$(<"${git_repository}/docs/openapi/baseline/general.json")" == '{"source":"working-tree-after-head-general"}' ]] || {
    echo "The general working-tree baseline fixture after the HEAD update was not changed" >&2
    exit 1
}

export OPENAPI_BREAKING_APPROVED=true
resolve_openapi_baselines "${git_repository}" "${temporary_directory}" > "${head_paths_file}" 2> "${resolution_log}"
[[ "$(<"${resolution_log}")" == "OpenAPI baselines at HEAD differ from '${base_ref}'; openapi-breaking-approved allows committed HEAD baselines" ]] || {
    echo "OPENAPI_BREAKING_APPROVED did not permit the committed HEAD baselines" >&2
    exit 1
}
[[ "$(<"${head_paths[0]}")" == '{"source":"head-general"}' ]] || {
    echo "OPENAPI_BREAKING_APPROVED did not select the changed committed HEAD general baseline" >&2
    exit 1
}
[[ "$(<"${head_paths[1]}")" == '{"source":"committed-admin"}' ]] || {
    echo "OPENAPI_BREAKING_APPROVED did not select the committed HEAD admin baseline" >&2
    exit 1
}
unset OPENAPI_BREAKING_APPROVED
[[ "$(<"${git_repository}/docs/openapi/baseline/admin.json")" == '{"source":"working-tree-after-head-admin"}' ]] || {
    echo "The admin working-tree baseline fixture after the HEAD update was not changed" >&2
    exit 1
}

bootstrap_repository="${test_directory}/bootstrap-repository"
bootstrap_output_directory="${test_directory}/bootstrap-output"
mkdir -p "${bootstrap_repository}" "${bootstrap_output_directory}"
git -C "${bootstrap_repository}" -c init.defaultBranch=main init --quiet
git -C "${bootstrap_repository}" config user.email "ci@example.com"
git -C "${bootstrap_repository}" config user.name "CI"
: > "${bootstrap_repository}/.gitkeep"
git -C "${bootstrap_repository}" add .gitkeep
git -C "${bootstrap_repository}" commit --quiet -m "before baselines"
base_without_baselines="$(git -C "${bootstrap_repository}" rev-parse HEAD)"

mkdir -p "${bootstrap_repository}/docs/openapi/baseline"
printf '%s\n' '{"source":"bootstrap-general"}' > "${bootstrap_repository}/docs/openapi/baseline/general.json"
printf '%s\n' '{"source":"bootstrap-admin"}' > "${bootstrap_repository}/docs/openapi/baseline/admin.json"
git -C "${bootstrap_repository}" add docs/openapi/baseline
git -C "${bootstrap_repository}" commit --quiet -m "bootstrap baselines"
printf '%s\n' '{"source":"bootstrap-working-tree-general"}' > "${bootstrap_repository}/docs/openapi/baseline/general.json"
printf '%s\n' '{"source":"bootstrap-working-tree-admin"}' > "${bootstrap_repository}/docs/openapi/baseline/admin.json"

export OPENAPI_BASE_REF="${base_without_baselines}"
bootstrap_paths_file="${bootstrap_output_directory}/resolved-paths"
resolve_openapi_baselines "${bootstrap_repository}" "${bootstrap_output_directory}" > "${bootstrap_paths_file}"
bootstrap_paths=()
while IFS= read -r bootstrap_path; do bootstrap_paths+=("${bootstrap_path}"); done < "${bootstrap_paths_file}"
[[ "${#bootstrap_paths[@]}" -eq 2 ]] || {
    echo "Expected general and admin bootstrap baseline paths" >&2
    exit 1
}
[[ "$(<"${bootstrap_paths[0]}")" == '{"source":"bootstrap-general"}' ]] || {
    echo "Bootstrap general baseline was not resolved from HEAD" >&2
    exit 1
}
[[ "$(<"${bootstrap_paths[1]}")" == '{"source":"bootstrap-admin"}' ]] || {
    echo "Bootstrap admin baseline was not resolved from HEAD" >&2
    exit 1
}
[[ "$(<"${bootstrap_repository}/docs/openapi/baseline/general.json")" == '{"source":"bootstrap-working-tree-general"}' ]] || {
    echo "The bootstrap general working-tree baseline fixture was not changed" >&2
    exit 1
}
[[ "$(<"${bootstrap_repository}/docs/openapi/baseline/admin.json")" == '{"source":"bootstrap-working-tree-admin"}' ]] || {
    echo "The bootstrap admin working-tree baseline fixture was not changed" >&2
    exit 1
}

printf '%s\n' '{"source":"partial-general"}' > "${bootstrap_repository}/docs/openapi/baseline/general.json"
partial_index="${test_directory}/partial-index"
GIT_INDEX_FILE="${partial_index}" git -C "${bootstrap_repository}" read-tree "${base_without_baselines}"
GIT_INDEX_FILE="${partial_index}" git -C "${bootstrap_repository}" add docs/openapi/baseline/general.json
partial_tree="$(GIT_INDEX_FILE="${partial_index}" git -C "${bootstrap_repository}" write-tree)"
partial_baseline_ref="$(printf '%s\n' 'partial baseline' | GIT_INDEX_FILE="${partial_index}" git -C "${bootstrap_repository}" commit-tree "${partial_tree}" -p "${base_without_baselines}")"
export OPENAPI_BASE_REF="${partial_baseline_ref}"
partial_paths_file="${bootstrap_output_directory}/partial-paths"
if resolve_openapi_baselines "${bootstrap_repository}" "${bootstrap_output_directory}" > "${partial_paths_file}" 2>/dev/null; then
    echo "An incomplete OpenAPI baseline pair was accepted" >&2
    exit 1
fi

export OPENAPI_BASE_REF="does-not-exist"
nonexistent_paths_file="${bootstrap_output_directory}/nonexistent-paths"
if resolve_openapi_baselines "${bootstrap_repository}" "${bootstrap_output_directory}" > "${nonexistent_paths_file}" 2>/dev/null; then
    echo "A nonexistent OpenAPI base ref was accepted" >&2
    exit 1
fi

echo "OpenAPI baseline source test passed"
