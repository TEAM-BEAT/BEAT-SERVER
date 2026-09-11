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

for required_command in k6 sops aws; do
  command -v "$required_command" >/dev/null || {
    echo "$required_command is required." >&2
    exit 69
  }
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(git -C "$script_dir" rev-parse --show-toplevel)"
data_file="${DATA_FILE:-$script_dir/cases.json}"
base_url="${BASE_URL:-https://api-dev.beatlive.kr}"
git_sha="${GIT_SHA:-$(git -C "$repo_root" rev-parse HEAD)}"
dev_inventory_secrets="$repo_root/ops/ansible/inventories/dev/group_vars/all/secrets.sops.yml"
aws_region="ap-northeast-2"

dev_host="$(sops --decrypt --extract '["ansible_host"]' "$dev_inventory_secrets")" || {
  echo "Unable to decrypt the dev deployment host from SOPS." >&2
  exit 78
}
if [[ ! "$dev_host" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
  echo "The dev deployment host must be an IPv4 address." >&2
  exit 78
fi

resolved_instance_id="$(aws ec2 describe-instances \
  --region "$aws_region" \
  --filters \
    "Name=network-interface.association.public-ip,Values=$dev_host" \
    "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].InstanceId' \
  --output text)" || {
  echo "Unable to resolve the dev deployment host through AWS." >&2
  exit 78
}
if [[ ! "$resolved_instance_id" =~ ^i-[0-9a-f]{8,17}$ ]]; then
  echo "The dev deployment host must resolve to exactly one running EC2 instance." >&2
  exit 78
fi

resolved_cpu_credits="$(aws ec2 describe-instance-credit-specifications \
  --region "$aws_region" \
  --instance-ids "$resolved_instance_id" \
  --query 'InstanceCreditSpecifications[0].CpuCredits' \
  --output text)" || {
  echo "Unable to resolve the dev EC2 CPU credit mode through AWS." >&2
  exit 78
}
case "$resolved_cpu_credits" in
  standard | unlimited) ;;
  *)
    echo "The dev EC2 CPU credit mode must be standard or unlimited." >&2
    exit 78
    ;;
esac

printf 'Verified dev EC2 instance %s with CPU credit mode %s.\n' \
  "$resolved_instance_id" "$resolved_cpu_credits"

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
EC2_INSTANCE_ID="$resolved_instance_id" \
EC2_CPU_CREDITS="$resolved_cpu_credits" \
LOAD_PROFILE="$profile" \
K6_OTEL_SERVICE_NAME="${K6_OTEL_SERVICE_NAME:-beat-k6}" \
K6_OTEL_METRIC_PREFIX="${K6_OTEL_METRIC_PREFIX:-k6_}" \
K6_OTEL_SINGLE_COUNTER_FOR_RATE="${K6_OTEL_SINGLE_COUNTER_FOR_RATE:-true}" \
k6 run "${k6_output_args[@]}" stock-contention.js
