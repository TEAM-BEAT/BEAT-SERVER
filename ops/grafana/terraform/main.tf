provider "grafana" {
  url    = var.grafana_url
  auth   = var.grafana_auth
  org_id = var.grafana_org_id
}

locals {
  prometheus_datasource = {
    type = "prometheus"
    uid  = var.prometheus_datasource_uid
  }

  expression_datasource = {
    type = "__expr__"
    uid  = "-100"
  }

  cloudwatch_datasource = {
    type = "cloudwatch"
    uid  = var.cloudwatch_datasource_uid
  }

  query_condition = {
    query    = { params = ["A", "5m", "now"] }
    reducer  = { params = [], type = "last" }
    operator = { type = "and" }
    type     = "query"
  }

  cloudwatch_metric = {
    datasource = local.cloudwatch_datasource
    dimensions = { DBInstanceIdentifier = var.rds_instance_identifier }
    matchExact = true
    namespace  = "AWS/RDS"
    period     = "300"
    region     = var.aws_region
    queryMode  = "Metrics"
  }
}

resource "grafana_contact_point" "beat_slack" {
  name = "beat-operations-slack"

  slack {
    url                     = var.slack_webhook_url
    title                   = "[BEAT] {{ template \"default.title\" . }}"
    text                    = "{{ template \"default.message\" . }}"
    disable_resolve_message = false
  }
}

resource "grafana_notification_policy" "beat" {
  contact_point   = grafana_contact_point.beat_slack.name
  group_by        = ["alertname", "env", "application", "severity"]
  group_wait      = "30s"
  group_interval  = "5m"
  repeat_interval = "4h"

  policy {
    matcher {
      label = "severity"
      match = "="
      value = "critical"
    }

    contact_point = grafana_contact_point.beat_slack.name
  }
}

resource "grafana_rule_group" "beat_prometheus" {
  name             = "BEAT application and pipeline"
  folder_uid       = var.alert_folder_uid
  interval_seconds = 30

  rule {
    name           = "BEAT application scrape down"
    condition      = "B"
    for            = "2m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "BEAT APIs are not being scraped"
      description = "No beat-apis application series is present for dev or prod."
    }
    labels = {
      severity = "critical"
      env      = "{{ $labels.env }}"
    }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 600
        to   = 0
      }
      datasource_uid = var.prometheus_datasource_uid
      model = jsonencode(merge({ datasource = local.prometheus_datasource }, {
        editorMode    = "code"
        expr          = "label_replace((max by (env) (up{env=~\"dev|prod\",job=\"apis\",module=\"apis\"}) or on (env) (label_replace(vector(0), \"env\", \"dev\", \"\", \"\") or label_replace(vector(0), \"env\", \"prod\", \"\", \"\"))) == bool 0, \"env\", \"$1\", \"env\", \"(dev|prod)\")"
        instant       = true
        intervalMs    = 1000
        maxDataPoints = 43200
        range         = false
        refId         = "A"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [0], type = "gt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }

  rule {
    name           = "BEAT Hikari pending connections"
    condition      = "B"
    for            = "1m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "A BEAT Hikari pool has pending connections"
      description = "Evaluate the maximum per instance and deployment color; Blue/Green pools are not summed."
    }
    labels = {
      severity    = "critical"
      env         = "{{ $labels.env }}"
      application = "{{ $labels.application }}"
    }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 600
        to   = 0
      }
      datasource_uid = var.prometheus_datasource_uid
      model = jsonencode(merge({ datasource = local.prometheus_datasource }, {
        editorMode    = "code"
        expr          = "max by (env, application, instance, color, pool) (hikaricp_connections_pending{env=~\"dev|prod\"})"
        instant       = false
        intervalMs    = 1000
        maxDataPoints = 43200
        range         = true
        refId         = "A"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [0], type = "gt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }

  rule {
    name           = "BEAT Application Success SLO fast burn"
    condition      = "B"
    for            = "5m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "BEAT application success SLO is fast-burning"
      description = "14-day 99.5% SLO: 1h and 5m burn rates both exceed 6.72x."
    }
    labels = {
      severity    = "critical"
      env         = "{{ $labels.env }}"
      application = "{{ $labels.application }}"
    }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 600
        to   = 0
      }
      datasource_uid = var.prometheus_datasource_uid
      model = jsonencode(merge({ datasource = local.prometheus_datasource }, {
        editorMode    = "code"
        expr          = "max by (env, application) (beat_application_fast_burn:14d)"
        instant       = false
        intervalMs    = 1000
        maxDataPoints = 43200
        range         = true
        refId         = "A"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [0], type = "gt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }

  rule {
    name           = "BEAT Application Success SLO slow burn"
    condition      = "B"
    for            = "30m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "BEAT application success SLO is slow-burning"
      description = "14-day 99.5% SLO: 6h and 30m burn rates both exceed 2.8x."
    }
    labels = {
      severity    = "warning"
      env         = "{{ $labels.env }}"
      application = "{{ $labels.application }}"
    }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 21600
        to   = 0
      }
      datasource_uid = var.prometheus_datasource_uid
      model = jsonencode(merge({ datasource = local.prometheus_datasource }, {
        editorMode    = "code"
        expr          = "max by (env, application) (beat_application_slow_burn:14d)"
        instant       = false
        intervalMs    = 1000
        maxDataPoints = 43200
        range         = true
        refId         = "A"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [0], type = "gt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }

  rule {
    name           = "BEAT Alloy remote-write failures"
    condition      = "B"
    for            = "5m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "Alloy is rejecting remote-write samples"
      description = "Remote-write failure counters must remain zero in dev and prod."
    }
    labels = {
      severity = "critical"
      env      = "{{ $labels.env }}"
    }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 600
        to   = 0
      }
      datasource_uid = var.prometheus_datasource_uid
      model = jsonencode(merge({ datasource = local.prometheus_datasource }, {
        editorMode    = "code"
        expr          = "sum by (env) (rate(prometheus_remote_storage_samples_failed_total{env=~\"dev|prod\"}[5m]))"
        instant       = false
        intervalMs    = 1000
        maxDataPoints = 43200
        range         = true
        refId         = "A"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [0], type = "gt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }
}

resource "grafana_rule_group" "beat_shared_rds" {
  name             = "BEAT shared RDS pressure"
  folder_uid       = var.alert_folder_uid
  interval_seconds = 60

  rule {
    name           = "BEAT shared RDS CPU pressure"
    condition      = "B"
    for            = "5m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "Shared RDS CPU is at or above 80%"
      description = "SHARED RDS — dev load can affect prod. CloudWatch cannot separate beatDev and beatProd schemas."
    }
    labels = { severity = "critical" }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 600
        to   = 0
      }
      datasource_uid = var.cloudwatch_datasource_uid
      model = jsonencode(merge({ datasource = local.cloudwatch_datasource }, local.cloudwatch_metric, {
        metricName = "CPUUtilization"
        refId      = "A"
        statistic  = "Average"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [80], type = "gt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }

  rule {
    name           = "BEAT shared RDS freeable memory"
    condition      = "B"
    for            = "5m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "Shared RDS freeable memory is at or below 128 MiB"
      description = "SHARED RDS — dev load can affect prod."
    }
    labels = { severity = "critical" }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 600
        to   = 0
      }
      datasource_uid = var.cloudwatch_datasource_uid
      model = jsonencode(merge({ datasource = local.cloudwatch_datasource }, local.cloudwatch_metric, {
        metricName = "FreeableMemory"
        refId      = "A"
        statistic  = "Minimum"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [134217728], type = "lt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }

  rule {
    name           = "BEAT shared RDS connection pressure"
    condition      = "B"
    for            = "5m"
    no_data_state  = "NoData"
    exec_err_state = "Error"
    annotations = {
      summary     = "Shared RDS connections exceed 80% of max_connections"
      description = "SHARED RDS — dev load can affect prod."
    }
    labels = { severity = "critical" }

    data {
      ref_id     = "A"
      query_type = ""
      relative_time_range {
        from = 600
        to   = 0
      }
      datasource_uid = var.cloudwatch_datasource_uid
      model = jsonencode(merge({ datasource = local.cloudwatch_datasource }, local.cloudwatch_metric, {
        metricName = "DatabaseConnections"
        refId      = "A"
        statistic  = "Maximum"
      }))
    }
    data {
      ref_id     = "B"
      query_type = ""
      relative_time_range {
        from = 0
        to   = 0
      }
      datasource_uid = "-100"
      model = jsonencode({
        conditions    = [merge(local.query_condition, { evaluator = { params = [var.rds_max_connections * 0.8], type = "gt" } })]
        datasource    = local.expression_datasource
        expression    = "A"
        intervalMs    = 1000
        maxDataPoints = 43200
        refId         = "B"
        type          = "classic_conditions"
      })
    }
  }
}
