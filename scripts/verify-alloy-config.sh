#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
alloy_image="grafana/alloy:v1.19.2"
work_dir="$(mktemp -d)"

cleanup() {
  rm -rf -- "$work_dir"
}
trap cleanup EXIT

command -v ansible-playbook >/dev/null || {
  echo "ansible-playbook is required" >&2
  exit 1
}

skip_image_validation="${SKIP_ALLOY_IMAGE_VALIDATION:-0}"
if [[ "$skip_image_validation" != "1" ]]; then
  command -v docker >/dev/null || {
    echo "docker is required unless SKIP_ALLOY_IMAGE_VALIDATION=1" >&2
    exit 1
  }
fi

render_playbook="$work_dir/render.yml"
cat >"$render_playbook" <<'YAML'
---
- name: Render Alloy configuration without deployment secrets
  hosts: localhost
  connection: local
  gather_facts: false
  vars_files:
    - "{{ alloy_validation_repo_root }}/ops/ansible/roles/observability_alloy/defaults/main.yml"
  vars:
    gc_prom_url: "https://prom.example.invalid/api/prom/push"
    gc_token: "synthetic-token"
    gc_loki_url: "https://logs.example.invalid/loki/api/v1/push"
    gc_loki_user: "synthetic-loki-user"
    gc_tempo_endpoint: "tempo.example.invalid:443"
    gc_tempo_user: "synthetic-tempo-user"
    actuator_port: 18081
    actuator_path: "/actuator"
    observability_alloy_container_secret_dir: "/etc/alloy/secrets"
    observability_alloy_enable_self_monitoring: true
    observability_alloy_enable_redis: true
    foundation_redis_enabled: true
    observability_alloy_redis_exporter_password: "synthetic-redis-password"
    observability_alloy_shared_mysql_enabled: false
    observability_alloy_mem_limit: "256m"
  tasks:
    - name: Render Alloy configuration
      ansible.builtin.template:
        src: "{{ alloy_validation_repo_root }}/ops/ansible/roles/observability_alloy/templates/config.alloy.j2"
        dest: "{{ alloy_validation_output_path }}"
        mode: "0600"
YAML

for environment in dev prod; do
  output_path="$work_dir/${environment}.alloy"
  if [[ "$environment" == "dev" ]]; then
    enable_cadvisor=false
    enable_k6=true
  else
    enable_cadvisor=true
    enable_k6=false
  fi

  ansible-playbook -i 'localhost,' "$render_playbook" \
    -e "alloy_validation_repo_root=$repo_root" \
    -e "alloy_validation_output_path=$output_path" \
    -e "deploy_environment=$environment" \
    -e "observability_alloy_cluster=beat-$environment" \
    -e "observability_alloy_enable_logs=true" \
    -e "observability_alloy_enable_traces=true" \
    -e "observability_alloy_enable_cadvisor=$enable_cadvisor" \
    -e "observability_alloy_enable_k6_metrics=$enable_k6"

  grep -q 'prometheus.exporter.self "alloy"' "$output_path"
  grep -q 'prometheus.exporter.redis "redis"' "$output_path"
  grep -q 'otelcol.processor.memory_limiter "apps"' "$output_path"
  grep -q 'otelcol.processor.batch "apps"' "$output_path"
  grep -q 'limit          = "64MiB"' "$output_path"
  grep -q 'spike_limit    = "16MiB"' "$output_path"
  if grep -Eq 'tail_sampling|sampling_percentage' "$output_path"; then
    echo "$environment config still contains removed sampling pipeline" >&2
    exit 1
  fi
  if [[ "$environment" == "dev" ]] && ! grep -q 'otelcol.receiver.otlp "k6"' "$output_path"; then
    echo "dev config is missing the k6 OTLP receiver" >&2
    exit 1
  fi
  if [[ "$environment" == "prod" ]] && grep -q 'otelcol.receiver.otlp "k6"' "$output_path"; then
    echo "prod config unexpectedly enables the k6 OTLP receiver" >&2
    exit 1
  fi
done

if [[ "$skip_image_validation" == "1" ]]; then
  echo "Rendered dev/prod Alloy configs (image validation skipped)"
  exit 0
fi

for environment in dev prod; do
  secret_dir="$work_dir/secrets-$environment"
  mkdir -m 700 "$secret_dir"
  for secret_name in gc_token gc_prom_user gc_loki_user gc_tempo_user redis_password; do
    printf '%s\n' "synthetic-$secret_name" >"$secret_dir/$secret_name"
    chmod 600 "$secret_dir/$secret_name"
  done

  docker run --rm --pull=always \
    --volume "$work_dir/$environment.alloy:/etc/alloy/config.alloy:ro" \
    --volume "$secret_dir:/etc/alloy/secrets:ro" \
    "$alloy_image" \
    validate --stability.level=generally-available /etc/alloy/config.alloy
done

echo "Rendered and validated dev/prod Alloy configs with $alloy_image"
