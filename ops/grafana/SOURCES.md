# Dashboard sources and ownership

The following Grafana dashboards were reviewed as reference material only.
They are not imported, copied wholesale, or treated as an upstream state
source. The BEAT dashboards are generated from `src/index.ts`.

| Reference | Revision | Panels/queries adapted |
| --- | --- | --- |
| [25359](https://grafana.com/grafana/dashboards/25359/) | Reference-only; revision not pinned in this repository | JVM and application signal layout ideas |
| [4701](https://grafana.com/grafana/dashboards/4701/) | Reference-only; revision not pinned in this repository | Node/host and resource panel organization; metric names are verified independently |
| [20729](https://grafana.com/grafana/dashboards/20729/) | Reference-only; revision not pinned in this repository | Redis signal grouping |

## Ownership

- Foundation SDK TypeScript in `src/` is the dashboard source of truth.
- Deterministic JSON in `generated/` is the Git Sync input.
- Mimir recording rules in `rules/recording/` are applied with `mimirtool` in namespace `beat`.
- Grafana-managed alert rules, the single Slack contact point, and notification policy are configured in the Grafana UI.
- A separate Alertmanager is not installed; Terraform is intentionally not part of this alert-management path.
- Grafana Cloud Mimir/Loki/Tempo datasources remain cloud-owned; the generated dashboards select datasource variables and commit no live UIDs.

## Load-test metric contract

`90-load-test.json` is generated from `src/index.ts`. Its k6 names follow the
official k6 `v1.4.0` OTLP exporter and the pinned Alloy `v1.19.2`
`otelcol.exporter.prometheus` behavior:

- `K6_OTEL_METRIC_PREFIX=k6_` prefixes the metric names.
- The k6 `Counter` `http_reqs` is queried as `k6_http_reqs`.
- A stock-contention `Counter` such as `stock_contention_bookings_accepted`
  is queried as `k6_stock_contention_bookings_accepted` for the same prefix
  reason.
- The k6 `Rate` `http_req_failed` is emitted from k6's `.total` discriminator;
  Prometheus normalizes it to `k6_http_req_failed_total`, with
  `condition="nonzero"` selecting failures. This assumes k6's default
  `K6_OTEL_SINGLE_COUNTER_FOR_RATE=true`; the deprecated pair-of-counters mode
  has different names and is not this dashboard's contract.
- k6 `Trend` values are OTLP histograms in milliseconds. With
  `add_metric_suffixes = false`, Prometheus exposes the classic
  `<name>_bucket`, `<name>_sum`, and `<name>_count` series. The dashboard uses
  `histogram_quantile` for an operational estimate only.

The exact references used for this contract are the [k6 OpenTelemetry output
documentation](https://grafana.com/docs/k6/latest/results-output/real-time/opentelemetry/),
[k6 OTLP exporter source](https://github.com/grafana/k6/tree/v1.4.0/internal/output/opentelemetry),
and [Alloy Prometheus exporter
documentation](https://grafana.com/docs/alloy/latest/reference/components/otelcol/otelcol.exporter.prometheus/).
If an Alloy upgrade changes suffix conversion, update the source-of-truth and
the generated dashboard together; do not infer a series name from a local
JSON summary.

The stock-contention JSON summary and the read-only database invariant remain
authoritative for exact TPS, accepted/sold-out counts, overselling, duplicate
booking, and exact percentiles. Grafana panels provide correlation and timing
visibility, not a replacement verdict.

The warm-cache panel uses the verified [mysqld_exporter `global_status`
collector](https://github.com/prometheus/mysqld_exporter/blob/main/collector/global_status.go)
names:
`mysql_global_status_innodb_buffer_pool_reads` and
`mysql_global_status_innodb_buffer_pool_read_requests`; it is intentionally
empty when the shared read-only MySQL exporter is disabled. RDS panels use the
AWS/RDS `CPUUtilization`, `FreeableMemory`, `SwapUsage`,
`DatabaseConnections`, `ReadLatency`, `WriteLatency`, `ReadIOPS`, `WriteIOPS`,
`DiskQueueDepth`, and `BurstBalance` metrics with a 60-second period during the
experiment. `BurstBalance` can be absent for storage configurations where AWS
does not publish it and must not be interpreted as zero. The optional EC2 panel
uses the AWS/EC2 `CPUCreditBalance` metric, an exact `InstanceId` textbox, and
its 300-second basic-monitoring period.
The names and monitoring-period constraints are from the [Amazon RDS
metrics](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/rds-metrics.html)
and [Amazon EC2 CloudWatch metrics](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/viewing_metrics_with_cloudwatch.html)
references.

## Runtime instrumentation contracts

- Alloy container metrics follow the official
  [`prometheus.exporter.cadvisor` Docker deployment](https://grafana.com/docs/alloy/latest/reference/components/prometheus/prometheus.exporter.cadvisor/),
  including the host rootfs, runtime, sysfs, Docker data, device mounts, and
  privileged mode required for container metadata rather than only the root
  cgroup.
- JDBC query spans use
  [`datasource-micrometer`](https://github.com/jdbc-observations/datasource-micrometer)
  2.x for Spring Boot 4.x. Only `QUERY` observations and OpenTelemetry spans
  are enabled; JDBC metrics are disabled to avoid adding active series.
- Mimir recording rules are synchronized with
  [`mimirtool rules sync`](https://grafana.com/docs/mimir/latest/manage/tools/mimirtool/)
  and a dedicated Grafana Cloud token scoped to `rules:read` and
  `rules:write`.
