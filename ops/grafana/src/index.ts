import { readFileSync, mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import {
  CustomVariableBuilder,
  DashboardBuilder,
  DatasourceVariableBuilder,
  QueryVariableBuilder,
  ThresholdsConfigBuilder,
  ThresholdsMode,
  TextBoxVariableBuilder,
  VariableHide,
  VariableRefresh,
  type Panel,
  type ThresholdsConfig,
  type VariableModel,
} from "@grafana/grafana-foundation-sdk/dashboard";
import type { Builder as FoundationBuilder, Dataquery } from "@grafana/grafana-foundation-sdk/cog";
import {
  MetricEditorMode,
  MetricQueryType,
  MetricsQueryBuilder as CloudWatchQuery,
} from "@grafana/grafana-foundation-sdk/cloudwatch";
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
const CLOUD_USAGE: DataSourceRef = {
  type: "prometheus",
  uid: "${DS_GRAFANA_CLOUD_USAGE}",
};
const GRAFANA_CLOUD_LOGS_DATASOURCE_REGEX =
  "/^grafanacloud-.*-logs$/i";
const FREE_PLAN_ACTIVE_SERIES_THRESHOLDS = new ThresholdsConfigBuilder()
  .mode(ThresholdsMode.Absolute)
  .steps([
    { value: null, color: "green" },
    { value: 7000, color: "orange" },
    { value: 10000, color: "red" },
  ]);
// Timeseries panels default to a red threshold at raw value 80. With
// percentunit (raw 0-1) that renders as 8000% and stretches the y-axis to
// 10000%, so percentunit panels always declare 0.8 explicitly.
const PERCENT_UNIT_THRESHOLDS = new ThresholdsConfigBuilder()
  .mode(ThresholdsMode.Absolute)
  .steps([
    { value: null, color: "green" },
    { value: 0.8, color: "red" },
  ]);

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
  regex?: string,
): Builder<VariableModel> =>
  (() => {
    const builder = new DatasourceVariableBuilder(name)
    .type(type)
    .label(label)
    .description(`${label} is selected at dashboard load; its UID is never committed.`);
    if (regex) builder.regex(regex);
    return builder;
  })();

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
  cloudUsage?: boolean;
  loadTest?: boolean;
  ec2Instance?: boolean;
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
      datasourceVariable(
        "DS_LOKI",
        "loki",
        "Loki",
        GRAFANA_CLOUD_LOGS_DATASOURCE_REGEX,
      ),
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
        )
        .defaultValue("beat-prod-database"),
    );
  }

  if (options.ec2Instance) {
    variables.push(
      new TextBoxVariableBuilder("ec2_instance_id")
        .label("Load generator EC2 instance")
        .description(
          "Required exact AWS/EC2 InstanceId for CPUCreditBalance. Leave blank when the load generator is not a burstable EC2 instance.",
        ),
    );
  }

  if (options.cloudUsage) {
    variables.push(
      datasourceVariable(
        "DS_GRAFANA_CLOUD_USAGE",
        "prometheus",
        "Grafana Cloud usage",
        "/^grafanacloud-usage$/",
      ),
    );
  }

  if (options.loadTest) {
    variables.push(
      queryVariable(
        "test_id",
        "Load test",
        "label_values(k6_http_reqs, test_id)",
        PROMETHEUS,
      ),
      queryVariable(
        "strategy",
        "Strategy",
        'label_values(k6_http_reqs{test_id=~"$test_id"}, strategy)',
        PROMETHEUS,
      ),
      queryVariable(
        "load_profile",
        "Load profile",
        'label_values(k6_http_reqs{test_id=~"$test_id"}, load_profile)',
        PROMETHEUS,
      ),
      queryVariable(
        "phase",
        "Experiment phase",
        'label_values(k6_stock_contention_requests_submitted{test_id=~"$test_id"}, phase)',
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
  datasource: DataSourceRef = PROMETHEUS,
): Builder<Dataquery> {
  const query = new PrometheusQuery()
    .refId(refId)
    .expr(expression)
    .datasource(datasource)
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
    datasource?: DataSourceRef;
    thresholds?: Builder<ThresholdsConfig>;
  } = {},
): PanelBuilder {
  const Panel = options.stat ? StatPanel : TimeseriesPanel;
  const panel = new Panel()
    .id(id)
    .title(title)
    .datasource(options.datasource ?? PROMETHEUS)
    .span(options.span ?? 12)
    .height(options.height ?? 8)
    .withTarget(
      promQuery(
        expression,
        options.refId ?? "A",
        options.legendFormat,
        options.instant ?? Boolean(options.stat),
        options.datasource ?? PROMETHEUS,
      ),
    );

  if (options.description) {
    panel.description(options.description);
  }
  if (options.unit) {
    panel.unit(options.unit);
  }
  if (options.thresholds) {
    panel.thresholds(options.thresholds);
  } else if (options.unit === "percentunit") {
    panel.thresholds(PERCENT_UNIT_THRESHOLDS);
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
    datasource?: DataSourceRef;
  } = {},
): PanelBuilder {
  const panel = new TimeseriesPanel()
    .id(id)
    .title(title)
    .datasource(options.datasource ?? PROMETHEUS)
    .span(options.span ?? 12)
    .height(options.height ?? 8)
    .withTarget(promQuery(firstExpression, "A", options.firstLegend, false, options.datasource ?? PROMETHEUS))
    .withTarget(promQuery(secondExpression, "B", options.secondLegend, false, options.datasource ?? PROMETHEUS));

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
  periodSeconds = 300,
): PanelBuilder {
  const query = new CloudWatchQuery()
    .metricQueryType(MetricQueryType.Search)
    .metricEditorMode(MetricEditorMode.Code)
    .id(`rds_${metricName.toLowerCase()}`)
    .refId("A")
    .region("ap-northeast-2")
    .namespace("AWS/RDS")
    .expression(
      `SEARCH('{AWS/RDS,DBInstanceIdentifier} MetricName=\"${metricName}\" DBInstanceIdentifier=\"$rds_instance_identifier\"', '${statistic}', ${periodSeconds})`,
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

function ec2CpuCreditPanel(id: number): PanelBuilder {
  const query = new CloudWatchQuery()
    .metricQueryType(MetricQueryType.Search)
    .metricEditorMode(MetricEditorMode.Code)
    .id("ec2_cpucreditbalance")
    .refId("A")
    .region("ap-northeast-2")
    .namespace("AWS/EC2")
    .expression(
      `SEARCH('{AWS/EC2,InstanceId} MetricName=\"CPUCreditBalance\" InstanceId=\"$ec2_instance_id\"', 'Average', 300)`,
    )
    .datasource(CLOUDWATCH);

  return new TimeseriesPanel()
    .id(id)
    .title("Load generator EC2 — CPU credit balance")
    .description(
      "Optional load-generator signal. CPUCreditBalance is an AWS/EC2 metric for burstable instances and is queried at its 5-minute basic-monitoring period; enter one exact InstanceId above.",
    )
    .datasource(CLOUDWATCH)
    .span(12)
    .height(8)
    .withTarget(query)
    .unit("short");
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
        `sum(rate(http_server_requests_seconds_count{${HTTP_SELECTOR},status=~"5.."}[$__rate_interval])) or 0 * sum(rate(http_server_requests_seconds_count{${HTTP_SELECTOR}}[$__rate_interval]))`,
        `sum(rate(http_server_requests_seconds_count{${HTTP_SELECTOR},status="429"}[$__rate_interval])) or 0 * sum(rate(http_server_requests_seconds_count{${HTTP_SELECTOR}}[$__rate_interval]))`,
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
        "Known Tempo search-display issue (cf. grafana/tempo#6762): the trace itself is complete, but the search result table may show an empty Service column. Click the trace ID to open the trace directly. Trace search is scoped by the OTel environment mapping; use trace-to-logs for the same trace ID.",
      ),
    )
    .withPanel(
      tempoSearchPanel(
        7,
        "Database spans",
        '{ resource.service.name =~ "beat-.*" && resource.deployment.environment.name = "$tempo_environment" && (span.db.system.name = "mysql" || span.db.system = "mysql") }',
        "JDBC/MySQL client spans are not instrumented yet, so this panel is expected to be empty. Use the InnoDB pre/post SQL snapshots for database evidence; SQL text is intentionally not used as a label or dashboard variable.",
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
      metricPanel(3, "Allocation rate", `sum by (instance, color) (rate(jvm_gc_memory_allocated_bytes_total{${SERVICE_SELECTOR}}[$__rate_interval]))`, {
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

function cadvisorMetric(
  sample: (selector: string) => string,
): string {
  const labeledSeries = (label: string, selector: string): string =>
    `sum by (container) (label_replace(${sample(selector)}, "container", "$1", "${label}", "(.+)"))`;

  return [
    labeledSeries(
      "container_label_com_docker_compose_service",
      'env=~"$env",container_label_com_docker_compose_service!=""',
    ),
    labeledSeries(
      "name",
      'env=~"$env",container_label_com_docker_compose_service="",name!=""',
    ),
  ].join(" or ");
}

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
    "EC2 node, Docker Compose containers, and environment-local Redis. Container metrics retain only the Compose service label.",
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
      metricPanel(
        5,
        "Container CPU",
        cadvisorMetric(
          (selector) =>
            `rate(container_cpu_usage_seconds_total{${selector}}[$__rate_interval])`,
        ),
        {
        unit: "percentunit",
          legendFormat: "{{container}}",
          description:
            "cAdvisor retains the Docker Compose service label; the name fallback supports older exporter series during rollout.",
        },
      ),
    )
    .withPanel(
      metricPanel(
        6,
        "Container memory",
        cadvisorMetric(
          (selector) =>
            `container_memory_working_set_bytes{${selector}}`,
        ),
        {
          unit: "bytes",
          legendFormat: "{{container}}",
          description:
            "cAdvisor is enabled for prod only; compose service is preferred, with a name fallback for exporter label compatibility.",
        },
      ),
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
\`env=dev|prod\` separates the environment-local EC2, container, and Redis signals. The shared MySQL/RDS instance belongs on **03 Shared RDS / MySQL**.
`,
      ),
    );
}

function pipeline(): DashboardBuilder {
  const builder = baseDashboard(
    "05 Observability Pipeline",
    "beat-observability-pipeline",
    "Alloy self-monitoring, scrape health, and remote-write pipeline pressure. Cloud usage is tracked against the Free plan outside the data plane.",
    { defaultEnvironment: "prod", cloudUsage: true },
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
      metricPanel(2, "Alloy resident memory", `alloy_resources_process_resident_memory_bytes{env=~"$env"}`, {
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
      metricPanel(6, "Alloy component health", `sum by (health_type) (alloy_component_controller_running_components{env=~"$env"})`, {
        unit: "short",
        legendFormat: "{{health_type}}",
        description: "Alloy does not expose a stable remote-write WAL-size metric; component health and pending samples are the supported pressure signals.",
      }),
    )
    .withPanel(
      metricPanel(7, "Dropped log entries", `sum by (env) (rate(loki_write_dropped_entries_total{env=~"$env"}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{env}}",
      }),
    )
    .withPanel(
      metricPanel(8, "Span receive failures", `sum by (env) (rate(otelcol_receiver_failed_spans_total{env=~"$env"}[$__rate_interval])) + sum by (env) (rate(otelcol_receiver_refused_spans_total{env=~"$env"}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{env}}",
        description: "Spans failed or refused at the Alloy receiver. 0 or an empty graph means no span loss (healthy).",
      }),
    )
    .withPanel(
      metricPanel(
        9,
        "Grafana Cloud active series",
        "sum(grafanacloud_instance_active_series)",
        {
          datasource: CLOUD_USAGE,
          span: 24,
          unit: "short",
          stat: true,
          thresholds: FREE_PLAN_ACTIVE_SERIES_THRESHOLDS,
          description:
            "Grafana Cloud Free plan allows 10,000 active series. This sums grafanacloud_instance_active_series across the usage datasource's id labels; keep below the 70% target (7,000).",
        },
      ),
    )
    .withPanel(
      textPanel(
        10,
        "Free plan guardrail",
        `
The first 14-day operating window targets **<70% of the Grafana Cloud Free plan limits** before adding more signals. The active-series stat uses the selected Grafana Cloud usage datasource and marks the 7,000 target and 10,000 Free plan limit.
`,
      ),
    );
}

function loadTest(): DashboardBuilder {
  const builder = baseDashboard(
    "90 Load Test",
    "beat-load-test",
    "k6 stock-contention test_id correlation with outcome counters, latency histograms, completion/drain timing, server RPS, JVM/Hikari saturation, shared RDS and an optional burstable load-generator signal. Default environment is dev; prod remains selectable with an explicit shared-RDS warning.",
    { defaultEnvironment: "dev", cloudwatch: true, loadTest: true, ec2Instance: true },
  );

  const k6Selector = 'test_id=~"$test_id",scenario=~"$test_scenario",strategy=~"$strategy",load_profile=~"$load_profile"';
  const experimentSelector = `${k6Selector},phase=~"$phase"`;
  const experimentGroup = "strategy, load_profile, phase";
  const k6Metric = (metric: string) => `k6_${metric}`;
  const experimentRate = (metric: string, extraSelector = "") =>
    `sum by (${experimentGroup}) (rate(${k6Metric(metric)}{${experimentSelector}${extraSelector}}[$__rate_interval]))`;
  const experimentQuantile = (metric: string, quantile: number) =>
    `histogram_quantile(${quantile}, sum by (le, ${experimentGroup}) (rate(${k6Metric(metric)}_bucket{${experimentSelector}}[$__rate_interval])))`;
  // k6 test_id is emitted by the external runner, not by Spring server metrics.
  // The server selector intentionally includes every /api route, including the
  // stock-contention endpoint after its /api prefix was added.
  const serverSelector = HTTP_SELECTOR;
  const rdsExperimentDescription =
    "AWS/RDS SEARCH is constrained to the entered DBInstanceIdentifier and uses a 60s period for the experiment. Shared RDS is still in scope when the selected target environment is dev.";

  return builder
    .withPanel(
      metricPanel(1, "k6 request rate", `sum(rate(k6_http_reqs{${k6Selector}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{strategy}} {{load_profile}}",
        description:
          "k6 Counter http_reqs is exported as k6_http_reqs by Alloy with add_metric_suffixes=false; select test_id, strategy and load profile before comparing runs.",
      }),
    )
    .withPanel(
      metricPanel(2, "k6 failed request rate", `100 * sum(rate(k6_http_req_failed_total{${k6Selector},condition="nonzero"}[$__rate_interval])) / sum(rate(k6_http_reqs{${k6Selector}}[$__rate_interval]))`, {
        unit: "percent",
        legendFormat: "failed",
        description:
          "k6 Rate http_req_failed is exported as k6_http_req_failed_total with condition=nonzero; this is transport/status failure visibility, not the stock outcome verdict.",
      }),
    )
    .withPanel(
      dualMetricPanel(
        3,
        "k6 latency p95 / p99",
        `histogram_quantile(0.95, sum by (le, strategy, load_profile) (rate(k6_http_req_duration_bucket{${k6Selector}}[$__rate_interval])))`,
        `histogram_quantile(0.99, sum by (le, strategy, load_profile) (rate(k6_http_req_duration_bucket{${k6Selector}}[$__rate_interval])))`,
        {
          unit: "ms",
          firstLegend: "{{strategy}} {{load_profile}} p95",
          secondLegend: "{{strategy}} {{load_profile}} p99",
          description:
            "k6 Trend http_req_duration is an OTLP histogram in milliseconds; with Alloy add_metric_suffixes=false its classic bucket series is k6_http_req_duration_bucket. Prometheus quantiles are estimates; the local k6 summary remains authoritative.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        4,
        "k6 VUs / dropped iterations",
        `max(k6_vus{${k6Selector}})`,
        `sum(rate(k6_dropped_iterations{${k6Selector}}[$__rate_interval]))`,
        {
          firstLegend: "VUs",
          secondLegend: "dropped iterations",
          unit: "short",
          description:
            "k6 dropped_iterations is a Counter and is exported without an added _total suffix. Any non-zero local dropped count invalidates the stock-contention run.",
        },
      ),
    )
    .withPanel(
      metricPanel(5, "Server-side RPS", `sum(rate(http_server_requests_seconds_count{${serverSelector}}[$__rate_interval]))`, {
        unit: "reqps",
        legendFormat: "{{application}}",
        description: "The business /api selector includes /api/internal/experiments/stock-contention/{strategy}/bookings; compare this server-side RPS with k6 request rate as separate signals.",
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
      metricPanel(7, "Server Hikari pending connections", `max by (application, instance, color, pool) (hikaricp_connections_pending{${HIKARI_SELECTOR}})`, {
        unit: "short",
        legendFormat: "{{application}} {{instance}} {{color}} {{pool}}",
        description: "Pending waiters indicate pool pressure even when active/max utilization has not reached 100%.",
      }),
    )
    .withPanel(
      metricPanel(8, "Server JVM heap", `100 * sum by (application, instance, color) (jvm_memory_used_bytes{area="heap",${SERVICE_SELECTOR}}) / sum by (application, instance, color) (jvm_memory_max_bytes{area="heap",${SERVICE_SELECTOR}})`, {
        unit: "percent",
        legendFormat: "{{application}} {{instance}} {{color}}",
      }),
    )
    .withPanel(
      cloudWatchSearchPanel(
        9,
        "Shared RDS — CPU during test",
        "CPUUtilization",
        "Average",
        "percent",
        `${rdsExperimentDescription} Stop the run if CPU is >=80% for 5m, freeable memory <=128MiB, or connections exceed 80% of max.`,
        60,
      ),
    )
    .withPanel(
      cloudWatchSearchPanel(
        10,
        "Shared RDS — freeable memory during test",
        "FreeableMemory",
        "Average",
        "bytes",
        rdsExperimentDescription,
        60,
      ),
    )
    .withPanel(
      cloudWatchSearchPanel(
        11,
        "Shared RDS — connections during test",
        "DatabaseConnections",
        "Average",
        "short",
        rdsExperimentDescription,
        60,
      ),
    )
    .withPanel(
      cloudWatchSearchPanel(
        12,
        "Shared RDS — read latency during test",
        "ReadLatency",
        "Average",
        "s",
        rdsExperimentDescription,
        60,
      ),
    )
    .withPanel(
      cloudWatchSearchPanel(
        13,
        "Shared RDS — write latency during test",
        "WriteLatency",
        "Average",
        "s",
        rdsExperimentDescription,
        60,
      ),
    )
    .withPanel(
      dualMetricPanel(
        14,
        "Stock outcomes — accepted / sold out",
        experimentRate("stock_contention_bookings_accepted"),
        experimentRate("stock_contention_bookings_sold_out"),
        {
          unit: "reqps",
          firstLegend: "{{strategy}} {{load_profile}} accepted",
          secondLegend: "{{strategy}} {{load_profile}} sold_out",
          description:
            "Custom k6 Counters are correlated by test_id, git_sha, strategy, phase and load_profile. The local JSON summary and read-only DB invariant decide exact accepted/sold_out counts.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        15,
        "Stock outcomes — conflict exhausted / lock timeout",
        experimentRate("stock_contention_conflict_exhausted"),
        experimentRate("stock_contention_lock_timeout"),
        {
          unit: "reqps",
          firstLegend: "{{strategy}} {{load_profile}} conflict",
          secondLegend: "{{strategy}} {{load_profile}} lock timeout",
          description:
            "Any conflict_exhausted or lock_timeout sample is a failed experiment outcome, even if the HTTP status is 200.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        16,
        "Stock outcomes — unexpected / timeout",
        experimentRate("stock_contention_unexpected_response"),
        experimentRate("stock_contention_timeouts"),
        {
          unit: "reqps",
          firstLegend: "{{strategy}} unexpected",
          secondLegend: "{{strategy}} timeout",
          description:
            "The timeout Counter is a terminal request timeout count. For the Rate-based request-timeout signal, see the next panel; both remain separate from local exact counts.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        17,
        "Stock request timeout rate / submitted rate",
        experimentRate("stock_contention_request_timeout_total", ',condition="nonzero"'),
        experimentRate("stock_contention_requests_submitted"),
        {
          unit: "reqps",
          firstLegend: "{{strategy}} request timeout rate",
          secondLegend: "{{strategy}} submitted",
          description:
            "Rate metrics retain k6's condition label; condition=nonzero selects timed-out requests. The submitted Counter is shown as a rate to make timeout volume comparable with the planned request stream.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        18,
        "Accepted latency p95 / p99",
        experimentQuantile("stock_contention_accepted_latency_ms", 0.95),
        experimentQuantile("stock_contention_accepted_latency_ms", 0.99),
        {
          unit: "ms",
          firstLegend: "{{strategy}} {{load_profile}} p95",
          secondLegend: "{{strategy}} {{load_profile}} p99",
          description:
            "Accepted-only k6 Trend histogram. The Prometheus p95/p99 are histogram_quantile estimates; exact accepted latency percentiles are preserved in the local JSON summary.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        19,
        "Attempt count p50 / p99",
        experimentQuantile("stock_contention_attempt_count", 0.5),
        experimentQuantile("stock_contention_attempt_count", 0.99),
        {
          unit: "short",
          firstLegend: "{{strategy}} {{load_profile}} p50",
          secondLegend: "{{strategy}} {{load_profile}} p99",
          description:
            "Response attemptCount Trend; values above one expose retry work. The local JSON summary is authoritative for exact samples and maxima.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        20,
        "Completion elapsed p95 / p99",
        experimentQuantile("stock_contention_completion_elapsed_ms", 0.95),
        experimentQuantile("stock_contention_completion_elapsed_ms", 0.99),
        {
          unit: "ms",
          firstLegend: "{{strategy}} {{load_profile}} p95",
          secondLegend: "{{strategy}} {{load_profile}} p99",
          description:
            "Elapsed milliseconds from k6 setup start until each response completed; this makes tail work after the arrival window visible.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        21,
        "Drain time p95 / p99",
        experimentQuantile("stock_contention_drain_time_ms", 0.95),
        experimentQuantile("stock_contention_drain_time_ms", 0.99),
        {
          unit: "ms",
          firstLegend: "{{strategy}} {{load_profile}} p95",
          secondLegend: "{{strategy}} {{load_profile}} p99",
          description:
            "max(0, completion elapsed - planned profile duration). Zero means the response completed inside the planned window; positive values show completion/drain tail. Exact max drain_time_ms remains in local JSON.",
        },
      ),
    )
    .withPanel(
      dualMetricPanel(
        22,
        "MySQL warm-cache support — physical reads / read requests",
        `rate(mysql_global_status_innodb_buffer_pool_reads{${MYSQL_SELECTOR}}[$__rate_interval])`,
        `rate(mysql_global_status_innodb_buffer_pool_read_requests{${MYSQL_SELECTOR}}[$__rate_interval])`,
        {
          unit: "reqps",
          firstLegend: "physical reads",
          secondLegend: "buffer-pool read requests",
          description:
            "mysqld_exporter global_status names: Innodb_buffer_pool_reads and Innodb_buffer_pool_read_requests. The panel is conditional on the shared read-only MySQL exporter being enabled; an empty panel is expected otherwise.",
        },
      ),
    )
    .withPanel(
      ec2CpuCreditPanel(23),
    )
    .withPanel(
      textPanel(
        24,
        "Load-test observability and safety contract",
        `
- Select \`test_id\` first, then strategy, load_profile and phase. User ID, access token and booking ID are never metric tags.
- k6 OTLP naming assumes Alloy \`otelcol.exporter.prometheus.k6 { add_metric_suffixes = false }\`: Counter names are unsuffixed, Rate \`.total\` names become \`_total\`, and Trend histograms expose \`_bucket/_sum/_count\` in milliseconds.
- Grafana histogram p95/p99 values are estimates from exported buckets. Exact outcome counts, TPS, overselling/duplicate checks and exact local percentiles come from the retained JSON summary plus read-only DB invariant query.
- The /api HTTP selector includes the stock-contention endpoint, so server-side RPS includes this experiment while remaining independent from k6 RPS.
- CloudWatch RDS panels use a 60s period and require the exact shared DBInstanceIdentifier. EC2 CPUCreditBalance uses the exact InstanceId above and a 300s basic-monitoring period; a blank selector intentionally yields no series.
- The warm-cache panel is supporting evidence only: shared MySQL exporter collection must be enabled and the physical/read-request comparison does not replace the local/DB verdict.
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
