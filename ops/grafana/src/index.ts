import { readFileSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import {
  CustomVariableBuilder,
  DashboardBuilder,
  DatasourceVariableBuilder,
  QueryVariableBuilder,
  TextBoxVariableBuilder,
  VariableHide,
  VariableRefresh,
  type Panel,
  type VariableModel,
} from "@grafana/grafana-foundation-sdk/dashboard";
import type { Builder as FoundationBuilder, Dataquery } from "@grafana/grafana-foundation-sdk/cog";
import { MetricsQueryBuilder as CloudWatchQuery } from "@grafana/grafana-foundation-sdk/cloudwatch";
import { DataqueryBuilder as LokiQuery } from "@grafana/grafana-foundation-sdk/loki";
import { DataqueryBuilder as PrometheusQuery } from "@grafana/grafana-foundation-sdk/prometheus";
import { DataqueryBuilder as TempoQuery } from "@grafana/grafana-foundation-sdk/tempo";
import { PanelBuilder as LogsPanel } from "@grafana/grafana-foundation-sdk/logs";
import { PanelBuilder as StatPanel } from "@grafana/grafana-foundation-sdk/stat";
import { PanelBuilder as TablePanel } from "@grafana/grafana-foundation-sdk/table";
import { TextMode } from "@grafana/grafana-foundation-sdk/text";
import { PanelBuilder as TextPanel } from "@grafana/grafana-foundation-sdk/text";
import { PanelBuilder as TimeseriesPanel } from "@grafana/grafana-foundation-sdk/timeseries";

const sourceDirectory = dirname(fileURLToPath(import.meta.url));
const generatedDirectory = join(sourceDirectory, "..", "generated");

type DataSourceRef = { type: string; uid: string };
type Builder<T> = FoundationBuilder<T>;
type PanelBuilder = Builder<Panel>;

const PROMETHEUS: DataSourceRef = {
  type: "prometheus",
  uid: "${DS_PROMETHEUS}",
};
const LOKI: DataSourceRef = { type: "loki", uid: "${DS_LOKI}" };
const TEMPO: DataSourceRef = { type: "tempo", uid: "${DS_TEMPO}" };
const CLOUDWATCH: DataSourceRef = {
  type: "cloudwatch",
  uid: "${DS_CLOUDWATCH}",
};

const ROUTE_EXCLUSIONS =
  "^/(actuator|health|metrics|v3/api-docs|swagger-ui)(/.*)?$|^(UNKNOWN|NOT_FOUND)$";
const BUSINESS_ROUTE = "^/api(/.*)?$";
const APPLICATION_SELECTOR =
  `env=~\"$env\",application=~\"$application\"`;
const HTTP_SELECTOR = `${APPLICATION_SELECTOR},uri=~\"${BUSINESS_ROUTE}\",uri!~\"${ROUTE_EXCLUSIONS}\"`;
const SERVICE_SELECTOR =
  `${APPLICATION_SELECTOR},instance=~\"$instance\",color=~\"$color\"`;
const SERVICE_HTTP_SELECTOR = `${HTTP_SELECTOR},instance=~\"$instance\",color=~\"$color\"`;
const HIKARI_SELECTOR =
  `env=~\"$env\",application=~\"$application\",instance=~\"$instance\",color=~\"$color\"`;
const MYSQL_SELECTOR = 'env="shared",scope="beat-shared-rds"';

const datasourceVariable = (
  name: string,
  type: string,
  label: string,
): Builder<VariableModel> =>
  new DatasourceVariableBuilder(name)
    .type(type)
    .label(label)
    .description(`${label} is selected at dashboard load; its UID is never committed.`);

function queryVariable(
  name: string,
  label: string,
  query: string,
  datasource: DataSourceRef,
  defaultValue?: string,
): Builder<VariableModel> {
  const builder = new QueryVariableBuilder(name)
    .label(label)
    .query(query)
    .datasource(datasource)
    .refresh(VariableRefresh.OnDashboardLoad)
    .multi(false)
    .includeAll(true)
    .allValue(".*");

  if (defaultValue) {
    builder.current({ text: defaultValue, value: defaultValue });
  }

  return builder;
}

function dashboardVariables(options: {
  defaultEnvironment: "dev" | "prod";
  logs?: boolean;
  traces?: boolean;
  cloudwatch?: boolean;
  loadTest?: boolean;
}): Builder<VariableModel>[] {
  const variables: Builder<VariableModel>[] = [
    datasourceVariable("DS_PROMETHEUS", "prometheus", "Prometheus / Mimir"),
    queryVariable(
      "env",
      "Environment",
      "label_values(http_server_requests_seconds_count, env)",
      PROMETHEUS,
      options.defaultEnvironment,
    ),
    queryVariable(
      "application",
      "Application",
      'label_values(http_server_requests_seconds_count{env=~"$env"}, application)',
      PROMETHEUS,
    ),
    queryVariable(
      "instance",
      "Instance",
      'label_values(http_server_requests_seconds_count{env=~"$env",application=~"$application"}, instance)',
      PROMETHEUS,
    ),
    queryVariable(
      "color",
      "Deployment color",
      'label_values(http_server_requests_seconds_count{env=~"$env",application=~"$application"}, color)',
      PROMETHEUS,
    ),
  ];

  if (options.logs) {
    variables.push(
      datasourceVariable("DS_LOKI", "loki", "Loki"),
      queryVariable(
        "module",
        "Log module",
        'label_values({env=~"$env"}, module)',
        LOKI,
      ),
    );
  }

  if (options.traces) {
    const tempoDefault =
      options.defaultEnvironment === "dev" ? "development" : "production";
    variables.push(
      datasourceVariable("DS_TEMPO", "tempo", "Tempo"),
      new CustomVariableBuilder("tempo_environment")
        .label("Tempo environment")
        .description(
          "Tempo uses the OTel standard deployment.environment.name values; dev maps to development and prod to production.",
        )
        .values("development,production")
        .current({ text: tempoDefault, value: tempoDefault }),
    );
  }

  if (options.cloudwatch) {
    variables.push(
      datasourceVariable("DS_CLOUDWATCH", "cloudwatch", "CloudWatch"),
      new TextBoxVariableBuilder("rds_instance_identifier")
        .label("Shared RDS instance")
        .description(
          "Required CloudWatch dimension filter. Enter the existing shared RDS DBInstanceIdentifier; panels never search all RDS instances.",
        ),
    );
  }

  if (options.loadTest) {
    variables.push(
      queryVariable(
        "test_id",
        "Load test",
        "label_values(k6_http_reqs_total, test_id)",
        PROMETHEUS,
      ),
      new TextBoxVariableBuilder("test_scenario")
        .label("Scenario (optional)")
        .description("Optional k6 scenario filter; leave empty to show every scenario.")
        .defaultValue(".*"),
    );
  }

  return variables;
}

function baseDashboard(
  title: string,
  uid: string,
  description: string,
  options: Parameters<typeof dashboardVariables>[0],
): DashboardBuilder {
  const builder = new DashboardBuilder(title)
    .uid(uid)
    .description(description)
    .tags(["beat", "generated", "observability"])
    .readonly()
    .refresh("30s")
    .time({ from: "now-6h", to: "now" })
    .version(1);

  for (const variable of dashboardVariables(options)) {
    builder.withVariable(variable);
  }

  return builder;
}

function promQuery(
  expression: string,
  refId = "A",
  legendFormat?: string,
  instant = false,
): Builder<Dataquery> {
  const query = new PrometheusQuery()
    .refId(refId)
    .expr(expression)
    .datasource(PROMETHEUS)
    .exemplar(true);

  if (legendFormat) {
    query.legendFormat(legendFormat);
  }

  if (instant) {
    query.instant();
  } else {
    query.range();
  }

  return query;
}

function metricPanel(
  id: number,
  title: string,
  expression: string,
  options: {
    description?: string;
    unit?: string;
    legendFormat?: string;
    stat?: boolean;
    instant?: boolean;
    span?: number;
    height?: number;
    refId?: string;
  } = {},
): PanelBuilder {
  const Panel = options.stat ? StatPanel : TimeseriesPanel;
  const panel = new Panel()
    .id(id)
    .title(title)
    .datasource(PROMETHEUS)
    .span(options.span ?? 12)
    .height(options.height ?? 8)
    .withTarget(
      promQuery(
        expression,
        options.refId ?? "A",
        options.legendFormat,
        options.instant ?? Boolean(options.stat),
      ),
    );

  if (options.description) {
    panel.description(options.description);
  }
  if (options.unit) {
    panel.unit(options.unit);
  }

  return panel;
}

function dualMetricPanel(
  id: number,
  title: string,
  firstExpression: string,
  secondExpression: string,
  options: {
    description?: string;
    unit?: string;
    firstLegend?: string;
    secondLegend?: string;
    span?: number;
    height?: number;
  } = {},
): PanelBuilder {
  const panel = new TimeseriesPanel()
    .id(id)
    .title(title)
    .datasource(PROMETHEUS)
    .span(options.span ?? 12)
    .height(options.height ?? 8)
    .withTarget(promQuery(firstExpression, "A", options.firstLegend))
    .withTarget(promQuery(secondExpression, "B", options.secondLegend));

  if (options.description) {
    panel.description(options.description);
  }
  if (options.unit) {
    panel.unit(options.unit);
  }

  return panel;
}

function logsPanel(
  id: number,
  title: string,
  expression: string,
  description: string,
): PanelBuilder {
  return new LogsPanel()
    .id(id)
    .title(title)
    .description(description)
    .datasource(LOKI)
    .span(24)
    .height(9)
    .withTarget(
      new LokiQuery()
        .refId("A")
        .expr(expression)
        .datasource(LOKI)
        .range(true),
    );
}

function tempoSearchPanel(
  id: number,
  title: string,
  search: string,
  description: string,
): PanelBuilder {
  return new TablePanel()
    .id(id)
    .title(title)
    .description(description)
    .datasource(TEMPO)
    .span(24)
    .height(10)
    .withTarget(
      new TempoQuery()
        .refId("A")
        .search(search)
        .limit(50)
        .datasource(TEMPO),
    );
}

function cloudWatchSearchPanel(
  id: number,
  title: string,
  metricName: string,
  statistic: string,
  unit: string,
  description: string,
): PanelBuilder {
  const query = new CloudWatchQuery()
    .id(`rds_${metricName.toLowerCase()}`)
    .refId("A")
    .region("ap-northeast-2")
    .namespace("AWS/RDS")
    .expression(
      `SEARCH('{AWS/RDS,DBInstanceIdentifier} MetricName=\"${metricName}\" DBInstanceIdentifier=\"$rds_instance_identifier\"', '${statistic}', 300)`,
    )
    .datasource(CLOUDWATCH);

  return new TimeseriesPanel()
    .id(id)
    .title(title)
    .description(description)
    .datasource(CLOUDWATCH)
    .span(12)
    .height(8)
    .withTarget(query)
    .unit(unit);
}

function textPanel(id: number, title: string, content: string): PanelBuilder {
  return new TextPanel()
    .id(id)
    .title(title)
    .span(24)
    .height(6)
    .mode(TextMode.Markdown)
    .content(content);
}

function addOverviewPanels(builder: DashboardBuilder): void {
  builder
    .withPanel(
      metricPanel(1, "Request rate (RPS)", `sum(rate(http_server_requests_seconds_count{${HTTP_SELECTOR}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "requests",
      }),
    )
    .withPanel(
      metricPanel(
        2,
        "Application Success SLI",
        `100 * min(beat_application_success_ratio:14d{env=~"$env",application=~"$application"})`,
        {
          unit: "percent",
          stat: true,
          description:
            "Valid business-template HTTP requests only. 5xx and capacity/load-shedding 429 are bad events; normal 4xx and domain 404 remain good responses.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        3,
        "5xx and 429 rate",
        `sum(rate(http_server_requests_seconds_count{${HTTP_SELECTOR},status=~"5.."}[$__rate_interval]))`,
        `sum(rate(http_server_requests_seconds_count{${HTTP_SELECTOR},status="429"}[$__rate_interval]))`,
        {
          unit: "reqps",
          firstLegend: "5xx",
          secondLegend: "429",
          description: "Capacity/load-shedding 429 is tracked separately from other client errors.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        4,
        "Request latency p95 / p99",
        `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{${HTTP_SELECTOR}}[$__rate_interval])))`,
        `histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{${HTTP_SELECTOR}}[$__rate_interval])))`,
        {
          unit: "s",
          firstLegend: "p95",
          secondLegend: "p99",
          description: "Quantiles are derived from Prometheus histogram buckets; client-side percentiles are intentionally not used.",
        },
      ),
    )
    .withPanel(
      metricPanel(
        5,
        "Hikari utilization (max per instance/color)",
        `max by (env, application, instance, color, pool) (hikaricp_connections_active{${HIKARI_SELECTOR}} / hikaricp_connections_max{${HIKARI_SELECTOR}})`,
        {
          unit: "percentunit",
          legendFormat: "{{application}} {{instance}} {{color}} {{pool}}",
          description: "Blue/Green is never summed. The maximum utilization for each instance and deployment color is the saturation signal.",
        },
      ),
    )
    .withPanel(
      metricPanel(
        6,
        "Hikari pending connections",
        `max by (env, application, instance, color, pool) (hikaricp_connections_pending{${HIKARI_SELECTOR}})`,
        {
          unit: "short",
          legendFormat: "{{application}} {{instance}} {{color}} {{pool}}",
          description: "Any sustained pending value is actionable at instance/color level, even while another color is idle.",
        },
      ),
    )
    .withPanel(
      metricPanel(
        7,
        "JVM heap used",
        `100 * sum by (env, application, instance, color) (jvm_memory_used_bytes{area="heap",${SERVICE_SELECTOR}}) / sum by (env, application, instance, color) (jvm_memory_max_bytes{area="heap",${SERVICE_SELECTOR}})`,
        {
          unit: "percent",
          legendFormat: "{{application}} {{instance}} {{color}}",
        },
      ),
    )
    .withPanel(
      cloudWatchSearchPanel(
        8,
        "Shared RDS — CPU",
        "CPUUtilization",
        "Average",
        "percent",
        "SHARED RDS — dev load can affect prod. CloudWatch cannot separate beatDev and beatProd schemas.",
      ),
    )
    .withPanel(
      logsPanel(
        9,
        "ERROR logs",
        '{env=~"$env",module=~"$module",level="ERROR"}',
        "Logs keep trace_id in the entry body; Loki labels remain bounded to env/cluster/host/module/level.",
      ),
    )
    .withPanel(
      textPanel(
        10,
        "Signal contract",
        `
**Scope:** \`env=dev|prod\` is the Prometheus/Loki operational label. OTel resources use \`deployment.environment.name=development|production\`.

**SLO:** Application Success SLI is measured for known business routes. External DNS/TLS/Nginx failures require the Phase 2 synthetic checks.

**Data safety:** user ID, booking ID, raw URL, SQL text and trace ID are never metric labels.
`,
      ),
    );
}

function overview(): DashboardBuilder {
  const builder = baseDashboard(
    "00 System Overview",
    "beat-system-overview",
    "Production-first BEAT overview. The shared RDS warning applies to every database signal.",
    { defaultEnvironment: "prod", logs: true, cloudwatch: true },
  );
  addOverviewPanels(builder);
  return builder;
}

function serviceDeepDive(): DashboardBuilder {
  const builder = baseDashboard(
    "01 Service Deep Dive",
    "beat-service-deep-dive",
    "Route-level RED, instance/color traffic, and trace search. Tempo environment values are mapped to OTel standard names.",
    { defaultEnvironment: "prod", logs: true, traces: true },
  );

  return builder
    .withPanel(
      metricPanel(1, "RPS by route", `sum by (uri) (rate(http_server_requests_seconds_count{${HTTP_SELECTOR}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{uri}}",
        span: 24,
      }),
    )
    .withPanel(
      metricPanel(2, "p95 latency by route", `histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket{${HTTP_SELECTOR}}[$__rate_interval])))`, {
        unit: "s",
        legendFormat: "{{uri}}",
        span: 24,
      }),
    )
    .withPanel(
      metricPanel(3, "HTTP status rate", `sum by (status) (rate(http_server_requests_seconds_count{${HTTP_SELECTOR}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{status}}",
      }),
    )
    .withPanel(
      metricPanel(4, "Traffic by instance and color", `sum by (instance, color) (rate(http_server_requests_seconds_count{${SERVICE_HTTP_SELECTOR}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{instance}} {{color}}",
      }),
    )
    .withPanel(
      metricPanel(5, "JVM GC pause rate", `sum by (instance, color) (rate(jvm_gc_pause_seconds_sum{${SERVICE_SELECTOR}}[$__rate_interval]))`, {
        unit: "s",
        legendFormat: "{{instance}} {{color}}",
      }),
    )
    .withPanel(
      tempoSearchPanel(
        6,
        "Outbound HTTP spans",
        '{ resource.service.name =~ "beat-.*" && resource.deployment.environment.name = "$tempo_environment" && name =~ "(?i).*http.*" }',
        "Trace search is scoped by the OTel environment mapping; use trace-to-logs for the same trace ID.",
      ),
    )
    .withPanel(
      tempoSearchPanel(
        7,
        "Database spans",
        '{ resource.service.name =~ "beat-.*" && resource.deployment.environment.name = "$tempo_environment" && (name =~ "(?i).*mysql.*" || name =~ "(?i).*jdbc.*") }',
        "DB span names only; SQL text is intentionally not used as a label or dashboard variable.",
      ),
    )
    .withPanel(
      logsPanel(
        8,
        "Route errors with trace context",
        '{env=~"$env",module=~"$module",level=~"ERROR|WARN"} |~ `$application`',
        "Use the log entry trace_id to pivot to Tempo. The application filter is applied to the log body because logs use module labels.",
      ),
    );
}

function jvmAndHikari(): DashboardBuilder {
  const builder = baseDashboard(
    "02 JVM & Hikari",
    "beat-jvm-hikari",
    "JVM allocation/GC and Hikari saturation. All pool ratios are evaluated per instance and deployment color.",
    { defaultEnvironment: "prod" },
  );

  return builder
    .withPanel(
      metricPanel(1, "Heap used / max", `100 * sum by (instance, color) (jvm_memory_used_bytes{area="heap",${SERVICE_SELECTOR}}) / sum by (instance, color) (jvm_memory_max_bytes{area="heap",${SERVICE_SELECTOR}})`, {
        unit: "percent",
        legendFormat: "{{instance}} {{color}}",
      }),
    )
    .withPanel(
      metricPanel(2, "Non-heap used", `sum by (instance, color) (jvm_memory_used_bytes{area="nonheap",${SERVICE_SELECTOR}})`, {
        unit: "bytes",
        legendFormat: "{{instance}} {{color}}",
      }),
    )
    .withPanel(
      metricPanel(3, "Allocation rate", `sum by (instance, color) (rate(jvm_memory_allocated_bytes_total{${SERVICE_SELECTOR}}[$__rate_interval]))`, {
        unit: "Bps",
        legendFormat: "{{instance}} {{color}}",
      }),
    )
    .withPanel(
      metricPanel(4, "GC pause rate", `sum by (instance, color) (rate(jvm_gc_pause_seconds_sum{${SERVICE_SELECTOR}}[$__rate_interval]))`, {
        unit: "s",
        legendFormat: "{{instance}} {{color}}",
      }),
    )
    .withPanel(
      metricPanel(5, "Live threads", `max by (instance, color) (jvm_threads_live_threads{${SERVICE_SELECTOR}})`, {
        unit: "short",
        legendFormat: "{{instance}} {{color}}",
      }),
    )
    .withPanel(
      metricPanel(6, "Hikari active / max", `max by (instance, color, pool) (hikaricp_connections_active{${HIKARI_SELECTOR}} / hikaricp_connections_max{${HIKARI_SELECTOR}})`, {
        unit: "percentunit",
        legendFormat: "{{instance}} {{color}} {{pool}}",
        description: "Do not aggregate blue and green pools. A max per instance/color exposes the active saturation point.",
      }),
    )
    .withPanel(
      metricPanel(7, "Hikari idle connections", `max by (instance, color, pool) (hikaricp_connections_idle{${HIKARI_SELECTOR}})`, {
        unit: "short",
        legendFormat: "{{instance}} {{color}} {{pool}}",
      }),
    )
    .withPanel(
      metricPanel(8, "Hikari pending connections", `max by (instance, color, pool) (hikaricp_connections_pending{${HIKARI_SELECTOR}})`, {
        unit: "short",
        legendFormat: "{{instance}} {{color}} {{pool}}",
        description: "Pending > 0 for one minute is an alert regardless of the other color's utilization.",
      }),
    );
}

const sharedRdsDescription =
  "SHARED RDS — dev load can affect prod. CloudWatch and MySQL exporter cannot separate beatDev and beatProd schemas at the instance level.";

function sharedRds(): DashboardBuilder {
  const builder = baseDashboard(
    "03 Shared RDS / MySQL",
    "beat-shared-rds-mysql",
    sharedRdsDescription,
    { defaultEnvironment: "prod", cloudwatch: true },
  );

  return builder
    .withPanel(cloudWatchSearchPanel(1, "Shared RDS — CPU", "CPUUtilization", "Average", "percent", sharedRdsDescription))
    .withPanel(cloudWatchSearchPanel(2, "Shared RDS — freeable memory", "FreeableMemory", "Average", "bytes", sharedRdsDescription))
    .withPanel(cloudWatchSearchPanel(3, "Shared RDS — connections", "DatabaseConnections", "Average", "short", sharedRdsDescription))
    .withPanel(cloudWatchSearchPanel(4, "Shared RDS — read IOPS", "ReadIOPS", "Average", "short", sharedRdsDescription))
    .withPanel(cloudWatchSearchPanel(5, "Shared RDS — write IOPS", "WriteIOPS", "Average", "short", sharedRdsDescription))
    .withPanel(cloudWatchSearchPanel(6, "Shared RDS — disk queue", "DiskQueueDepth", "Average", "short", sharedRdsDescription))
    .withPanel(cloudWatchSearchPanel(7, "Shared RDS — burst balance", "BurstBalance", "Minimum", "percent", sharedRdsDescription))
    .withPanel(
      metricPanel(8, "MySQL queries per second", `rate(mysql_global_status_questions{${MYSQL_SELECTOR}}[$__rate_interval])`, {
        unit: "reqps",
        legendFormat: "questions",
        description: sharedRdsDescription,
      }),
    )
    .withPanel(
      metricPanel(9, "MySQL connections", `mysql_global_status_threads_connected{${MYSQL_SELECTOR}}`, {
        unit: "short",
        legendFormat: "connected",
        description: sharedRdsDescription,
      }),
    )
    .withPanel(
      dualMetricPanel(
        10,
        "MySQL slow queries / deadlocks",
        `rate(mysql_global_status_slow_queries{${MYSQL_SELECTOR}}[$__rate_interval])`,
        `rate(mysql_global_status_innodb_deadlocks{${MYSQL_SELECTOR}}[$__rate_interval])`,
        {
          unit: "reqps",
          firstLegend: "slow queries",
          secondLegend: "deadlocks",
          description: sharedRdsDescription,
        },
      ),
    )
    .withPanel(
      metricPanel(11, "InnoDB buffer pool data ratio", `sum by (env, scope, instance) (mysql_global_status_buffer_pool_pages{${MYSQL_SELECTOR},state="data"}) / sum by (env, scope, instance) (mysql_global_status_buffer_pool_pages{${MYSQL_SELECTOR},state=~"data|free|misc|old"})`, {
        unit: "percentunit",
        legendFormat: "buffer pool data ratio",
        description: sharedRdsDescription,
      }),
    )
    .withPanel(
      textPanel(
        12,
        "Shared RDS operating rule",
        `
**This is one database instance for both environments.**

- Treat \`env=shared\` and \`scope=beat-shared-rds\` as mandatory labels for exporter metrics.
- Investigate dev and prod deployments together when CPU, freeable memory, connections or queue pressure rises.
- Query samples, SQL text, and explain plans are intentionally out of scope for this phase.
`,
      ),
    );
}

function infrastructure(): DashboardBuilder {
  const builder = baseDashboard(
    "04 Infrastructure",
    "beat-infrastructure",
    "EC2 node, production containers, and environment-local Redis. dev cAdvisor is intentionally not collected.",
    { defaultEnvironment: "prod" },
  );

  return builder
    .withPanel(
      metricPanel(1, "Node CPU", `100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle",env=~"$env"}[$__rate_interval])))`, {
        unit: "percent",
        legendFormat: "{{instance}}",
      }),
    )
    .withPanel(
      metricPanel(2, "Node memory used", `100 * (1 - node_memory_MemAvailable_bytes{env=~"$env"} / node_memory_MemTotal_bytes{env=~"$env"})`, {
        unit: "percent",
        legendFormat: "{{instance}}",
      }),
    )
    .withPanel(
      metricPanel(3, "Node filesystem free", `100 * node_filesystem_avail_bytes{env=~"$env",fstype!~"tmpfs|overlay"} / node_filesystem_size_bytes{env=~"$env",fstype!~"tmpfs|overlay"}`, {
        unit: "percent",
        legendFormat: "{{instance}} {{mountpoint}}",
      }),
    )
    .withPanel(
      dualMetricPanel(
        4,
        "Node network receive / transmit",
        `sum by (instance) (rate(node_network_receive_bytes_total{env=~"$env",device!="lo"}[$__rate_interval]))`,
        `sum by (instance) (rate(node_network_transmit_bytes_total{env=~"$env",device!="lo"}[$__rate_interval]))`,
        { unit: "Bps", firstLegend: "receive", secondLegend: "transmit" },
      ),
    )
    .withPanel(
      metricPanel(5, "Container CPU (prod only)", `sum by (name) (rate(container_cpu_usage_seconds_total{env=~"$env",name!=""}[$__rate_interval]))`, {
        unit: "percentunit",
        legendFormat: "{{name}}",
        description: "cAdvisor is enabled for prod only; an empty dev panel is expected and documented by the dashboard description.",
      }),
    )
    .withPanel(
      metricPanel(6, "Container memory (prod only)", `sum by (name) (container_memory_working_set_bytes{env=~"$env",name!=""})`, {
        unit: "bytes",
        legendFormat: "{{name}}",
        description: "cAdvisor is enabled for prod only; an empty dev panel is expected and documented by the dashboard description.",
      }),
    )
    .withPanel(
      metricPanel(7, "Redis up", `redis_up{env=~"$env"}`, {
        unit: "short",
        legendFormat: "{{instance}}",
        stat: true,
      }),
    )
    .withPanel(
      metricPanel(8, "Redis commands processed", `rate(redis_commands_processed_total{env=~"$env"}[$__rate_interval])`, {
        unit: "reqps",
        legendFormat: "{{instance}}",
      }),
    )
    .withPanel(
      metricPanel(9, "Redis connected clients", `redis_connected_clients{env=~"$env"}`, {
        unit: "short",
        legendFormat: "{{instance}}",
      }),
    )
    .withPanel(
      textPanel(
        10,
        "Collection boundary",
        `
\`env=dev|prod\` separates the environment-local EC2 and Redis signals. The shared MySQL/RDS instance belongs on **03 Shared RDS / MySQL**.

The dev host intentionally does not collect cAdvisor container metrics; do not interpret those two panels as an outage.
`,
      ),
    );
}

function pipeline(): DashboardBuilder {
  const builder = baseDashboard(
    "05 Observability Pipeline",
    "beat-observability-pipeline",
    "Alloy self-monitoring, scrape health, and remote-write pipeline pressure. Cloud usage is tracked against the Free plan outside the data plane.",
    { defaultEnvironment: "prod" },
  );

  return builder
    .withPanel(
      metricPanel(1, "Alloy self scrape", `alloy_build_info{env=~"$env"}`, {
        unit: "short",
        legendFormat: "{{instance}}",
        stat: true,
        description: "Requires the Alloy self exporter/scrape component in the selected environment.",
      }),
    )
    .withPanel(
      metricPanel(2, "Alloy resident memory", `process_resident_memory_bytes{job=~".*alloy.*",env=~"$env"}`, {
        unit: "bytes",
        legendFormat: "{{instance}}",
      }),
    )
    .withPanel(
      metricPanel(3, "Scrape targets up", `sum by (job) (up{env=~"$env"})`, {
        unit: "short",
        legendFormat: "{{job}}",
      }),
    )
    .withPanel(
      metricPanel(4, "Remote-write failed samples", `sum by (remote_name) (rate(prometheus_remote_storage_samples_failed_total{env=~"$env"}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{remote_name}}",
      }),
    )
    .withPanel(
      metricPanel(5, "Remote-write pending samples", `sum by (remote_name) (prometheus_remote_storage_samples_pending{env=~"$env"})`, {
        unit: "short",
        legendFormat: "{{remote_name}}",
      }),
    )
    .withPanel(
      metricPanel(6, "Remote-write WAL size", `sum by (remote_name) (prometheus_remote_storage_wal_storage_size_bytes{env=~"$env"})`, {
        unit: "bytes",
        legendFormat: "{{remote_name}}",
      }),
    )
    .withPanel(
      metricPanel(7, "Dropped log entries", `sum by (env) (rate(loki_write_dropped_entries_total{env=~"$env"}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{env}}",
      }),
    )
    .withPanel(
      metricPanel(8, "Dropped spans", `sum by (env) (rate(otelcol_processor_dropped_spans_total{env=~"$env"}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{env}}",
      }),
    )
    .withPanel(
      textPanel(
        9,
        "Free plan guardrail",
        `
The first 14-day operating window targets **<70% of the Grafana Cloud Free plan limits** before adding more signals. Check Cloud Usage in the Grafana Cloud portal for the authoritative active-series and logs/traces usage numbers.

This dashboard deliberately does not invent a Cloud Usage metric name or datasource UID.
`,
      ),
    );
}

function loadTest(): DashboardBuilder {
  const builder = baseDashboard(
    "90 Load Test",
    "beat-load-test",
    "k6 test_id correlation with server RPS, JVM/Hikari saturation and shared RDS. Default environment is dev; prod remains selectable with an explicit shared-RDS warning.",
    { defaultEnvironment: "dev", cloudwatch: true, loadTest: true },
  );

  const loadSelector = 'test_id=~"$test_id",scenario=~"$test_scenario"';
  // k6 test_id is emitted by the external runner, not by Spring server metrics.
  // Keep server signals on the selected time range so they can be overlaid with k6.
  const serverSelector = HTTP_SELECTOR;

  return builder
    .withPanel(
      metricPanel(1, "k6 request rate", `sum(rate(k6_http_reqs_total{${loadSelector}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{scenario}}",
      }),
    )
    .withPanel(
      metricPanel(2, "k6 failed request rate", `100 * sum(rate(k6_http_req_failed{${loadSelector}}[$__rate_interval])) / sum(rate(k6_http_reqs_total{${loadSelector}}[$__rate_interval]))`, {
        unit: "percent",
        legendFormat: "failed",
      }),
    )
    .withPanel(
      dualMetricPanel(
        3,
        "k6 latency p95 / p99",
        `histogram_quantile(0.95, sum by (le) (rate(k6_http_req_duration_seconds_bucket{${loadSelector}}[$__rate_interval])))`,
        `histogram_quantile(0.99, sum by (le) (rate(k6_http_req_duration_seconds_bucket{${loadSelector}}[$__rate_interval])))`,
        { unit: "s", firstLegend: "p95", secondLegend: "p99" },
      ),
    )
    .withPanel(
      dualMetricPanel(
        4,
        "k6 VUs / dropped iterations",
        `max(k6_vus{${loadSelector}})`,
        `sum(rate(k6_dropped_iterations_total{${loadSelector}}[$__rate_interval]))`,
        { firstLegend: "VUs", secondLegend: "dropped iterations", unit: "short" },
      ),
    )
    .withPanel(
      metricPanel(5, "Server-side RPS", `sum(rate(http_server_requests_seconds_count{${serverSelector}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{application}}",
        description: "Compare trend with k6 request rate; k6 and server RPS are separate signals.",
      }),
    )
    .withPanel(
      metricPanel(6, "Server Hikari utilization", `max by (application, instance, color, pool) (hikaricp_connections_active{${HIKARI_SELECTOR}} / hikaricp_connections_max{${HIKARI_SELECTOR}})`, {
        unit: "percentunit",
        legendFormat: "{{application}} {{instance}} {{color}} {{pool}}",
        description: "Blue/Green pools are evaluated independently; sum(active)/sum(max) is intentionally not used.",
      }),
    )
    .withPanel(
      metricPanel(7, "Server JVM heap", `100 * sum by (application, instance, color) (jvm_memory_used_bytes{area="heap",${SERVICE_SELECTOR}}) / sum by (application, instance, color) (jvm_memory_max_bytes{area="heap",${SERVICE_SELECTOR}})`, {
        unit: "percent",
        legendFormat: "{{application}} {{instance}} {{color}}",
      }),
    )
    .withPanel(
      cloudWatchSearchPanel(
        8,
        "Shared RDS — CPU during test",
        "CPUUtilization",
        "Average",
        "percent",
        "SHARED RDS — dev load can affect prod. Stop the run if CPU is >=80% for 5m, freeable memory <=128MiB, or connections exceed 80% of max.",
      ),
    )
    .withPanel(
      textPanel(
        9,
        "Load-test safety contract",
        `
- \`test_id\` is the only correlation selector; user ID and booking ID are never metric tags.
- The run must be preflighted against the environment allowlist and workload budget before traffic starts.
- Shared RDS is always in scope, including when the selected target environment is \`dev\`.
- Keep the local JSON summary/artifact after the 14-day metrics window expires.
`,
      ),
    );
}

const dashboards: Array<{ file: string; build: () => DashboardBuilder }> = [
  { file: "00-system-overview.json", build: overview },
  { file: "01-service-deep-dive.json", build: serviceDeepDive },
  { file: "02-jvm-hikari.json", build: jvmAndHikari },
  { file: "03-shared-rds-mysql.json", build: sharedRds },
  { file: "04-infrastructure.json", build: infrastructure },
  { file: "05-observability-pipeline.json", build: pipeline },
  { file: "90-load-test.json", build: loadTest },
];

function sortKeys(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(sortKeys);
  }
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([key, child]) => [key, sortKeys(child)]),
    );
  }
  return value;
}

function renderJson(value: unknown): string {
  return `${JSON.stringify(sortKeys(value), null, 2)}\n`;
}

function writeOrCheck(file: string, expected: string, checkOnly: boolean): void {
  if (checkOnly) {
    let actual: string;
    try {
      actual = readFileSync(file, "utf8");
    } catch {
      throw new Error(`Generated dashboard is missing: ${file}`);
    }
    if (actual !== expected) {
      throw new Error(`Generated dashboard is stale: ${file}; run npm run generate`);
    }
    return;
  }

  writeFileSync(file, expected, "utf8");
}

function main(): void {
  const checkOnly = process.argv.includes("--check");
  mkdirSync(generatedDirectory, { recursive: true });

  for (const dashboard of dashboards) {
    const file = join(generatedDirectory, dashboard.file);
    writeOrCheck(file, renderJson(dashboard.build().build()), checkOnly);
  }
}

main();
