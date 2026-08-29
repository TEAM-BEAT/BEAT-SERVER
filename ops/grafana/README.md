# BEAT Grafana observability

`src/index.ts` uses the Grafana Foundation SDK to generate the seven dashboard
JSON files under `generated/`. Keep the generated files committed and run:

```bash
npm ci
npm run typecheck
npm run generate
npm run check:generated
```

## Ownership

- Dashboards and folders: Grafana Git Sync, pointing only at `generated/`.
- Recording rules: Mimir, applied with `mimirtool` in namespace `beat`.
- Alert rules, the notification policy, and one Slack contact point: Grafana-managed resources configured in the Grafana UI.
- Grafana Cloud Mimir, Loki, and Tempo datasources: Grafana Cloud-owned.
- Separate Alertmanager: not installed; all Slack notifications use the single Grafana contact point.

The alert plane is intentionally not managed by Terraform. Keep the following
inventory aligned with the Grafana UI configuration:

| Alert | Condition | For | Severity |
| --- | --- | --- | --- |
| BEAT application scrape down | `up{job="apis",module="apis"}` is `0` for an environment | 2m | critical |
| BEAT Hikari pending connections | Per `env/application/instance/color/pool`, pending `> 0` | 1m | critical |
| BEAT Application Success SLO fast burn | 1h and 5m burn rates both `> 6.72x` for the 14-day 99.5% SLO | 5m | critical |
| BEAT Application Success SLO slow burn | 6h and 30m burn rates both `> 2.8x` | 30m | warning |
| BEAT Alloy remote-write failures | `rate(prometheus_remote_storage_samples_failed_total[5m]) > 0` | 5m | critical |
| BEAT shared RDS CPU pressure | CloudWatch `CPUUtilization >= 80%` | 5m | critical |
| BEAT shared RDS freeable memory | CloudWatch `FreeableMemory <= 128 MiB` | 5m | critical |
| BEAT shared RDS connection pressure | CloudWatch `DatabaseConnections > 80%` of `max_connections` | 5m | critical |

For application scrape-down, the Grafana UI query must preserve both environment
labels and treat a missing `up` series as zero before testing for zero:

```promql
(
  max by (env) (up{env=~"dev|prod",job="apis",module="apis"})
  or on (env) (
    label_replace(vector(0), "env", "dev", "", "")
    or label_replace(vector(0), "env", "prod", "", "")
  )
) == bool 0
```

The RDS alerts are shared-scope alerts: dev load can affect prod, and
CloudWatch cannot separate the `beatDev` and `beatProd` schemas. Configure
Grafana alert rules with `No data` and query error states treated as alerting,
and group Slack notifications by `alertname`, `env`, `application`, and
`severity`. For the connection-pressure rule, calculate the 80% threshold from
an operator-observed and approved `max_connections` value; record that value
and its source in the Grafana rule annotation or runbook. No numeric
`max_connections` value is committed here.

See `SOURCES.md` for reference dashboard provenance.
