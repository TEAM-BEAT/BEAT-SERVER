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
- Terraform owns Grafana-managed alert rules, contact points, and notification policies.
- Grafana Cloud Mimir/Loki/Tempo datasources remain cloud-owned; the generated dashboards select datasource variables and commit no live UIDs.
