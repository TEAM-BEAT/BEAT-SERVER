#!/usr/bin/env bash

set -euo pipefail

usage() {
  echo "Usage: $0 <PESSIMISTIC|OPTIMISTIC|REDIS|ATOMIC> <warmup|flash> <test-id>" >&2
}

if [[ $# -ne 3 ]]; then
  usage
  exit 64
fi

strategy="$1"
profile="$2"
test_id="$3"

case "$strategy" in
  PESSIMISTIC | OPTIMISTIC | REDIS | ATOMIC) ;;
  *)
    usage
    exit 64
    ;;
esac

case "$profile" in
  warmup | flash) ;;
  *)
    usage
    exit 64
    ;;
esac

if [[ -z "$test_id" || "$test_id" =~ [[:space:]] ]]; then
  echo "test-id must be non-empty and contain no whitespace." >&2
  exit 64
fi

command -v k6 >/dev/null || {
  echo "k6 is required." >&2
  exit 69
}

if [[ ! "${EC2_INSTANCE_ID:-}" =~ ^i-[0-9a-f]{8,17}$ ]]; then
  echo "EC2_INSTANCE_ID must be the actual dev EC2 instance ID." >&2
  exit 64
fi

case "${EC2_CPU_CREDITS:-}" in
  standard | unlimited) ;;
  *)
    echo "EC2_CPU_CREDITS must be exactly standard or unlimited." >&2
    exit 64
    ;;
esac

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(git -C "$script_dir" rev-parse --show-toplevel)"
data_file="${DATA_FILE:-$script_dir/cases.json}"
base_url="${BASE_URL:-https://api-dev.beatlive.kr}"
git_sha="${GIT_SHA:-$(git -C "$repo_root" rev-parse HEAD)}"

if [[ ! -r "$data_file" ]]; then
  echo "Dataset is not readable: $data_file" >&2
  exit 66
fi

k6_output_args=()
if [[ -n "${K6_OUTPUT:-}" ]]; then
  k6_output_args=(--out "$K6_OUTPUT")
fi

cd "$script_dir"
TARGET_ENV=dev \
BASE_URL="$base_url" \
STRATEGY="$strategy" \
DATA_FILE="$data_file" \
TEST_ID="$test_id" \
GIT_SHA="$git_sha" \
EC2_INSTANCE_ID="$EC2_INSTANCE_ID" \
EC2_CPU_CREDITS="$EC2_CPU_CREDITS" \
LOAD_PROFILE="$profile" \
K6_OTEL_SERVICE_NAME="${K6_OTEL_SERVICE_NAME:-beat-k6}" \
K6_OTEL_METRIC_PREFIX="${K6_OTEL_METRIC_PREFIX:-k6_}" \
K6_OTEL_SINGLE_COUNTER_FOR_RATE="${K6_OTEL_SINGLE_COUNTER_FOR_RATE:-true}" \
k6 run "${k6_output_args[@]}" stock-contention.js
