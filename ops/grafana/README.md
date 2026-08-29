# BEAT Grafana as code

`src/index.ts` uses the Grafana Foundation SDK to generate the seven dashboard
JSON files under `generated/`. Keep the generated files committed and run:

```bash
npm ci
npm run typecheck
npm run generate
npm run check:generated
```

Git Sync should point only at `ops/grafana/generated`. The Terraform directory
contains the Grafana-managed alert plane; it does not recreate cloud-owned
Mimir, Loki, or Tempo datasources. See `SOURCES.md` for reference dashboard
provenance.
