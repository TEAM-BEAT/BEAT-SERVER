# Grafana-managed alert plane

This module owns Grafana-managed alert rules, the Slack contact point, and the
notification policy only. It intentionally has no `grafana_data_source`
resource: Grafana Cloud's Mimir, Loki, and Tempo datasources remain
platform-owned.

The CloudWatch datasource must already exist. Before adding it to Terraform
state, run an import plan and confirm that the plan has no replacement:

```text
terraform import grafana_data_source.cloudwatch <existing-id>
terraform plan -var-file=secrets.tfvars
```

This scaffold does not declare that resource, so an operator can keep the
CloudWatch datasource platform-owned or add a separately reviewed import-only
resource. Never commit the stack URL, token, webhook, datasource UID, or RDS
identifier; inject them through CI secrets or an ignored tfvars file.

Apply after the `beat` Mimir recording rules have passed `mimirtool rules
lint/check/diff`. Dashboard and folder ownership stays with Git Sync.
