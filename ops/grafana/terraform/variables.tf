variable "grafana_url" {
  type        = string
  description = "Grafana Cloud stack URL. Do not commit a value."
}

variable "grafana_auth" {
  type        = string
  sensitive   = true
  description = "Grafana service-account token. Inject through a secret-backed tfvars file or CI secret."
}

variable "grafana_org_id" {
  type        = number
  default     = null
  nullable    = true
  description = "Optional Grafana organization ID for self-hosted Grafana."
}

variable "alert_folder_uid" {
  type        = string
  description = "Existing folder UID. Folder/dashboard ownership stays with Git Sync."
}

variable "prometheus_datasource_uid" {
  type        = string
  description = "Existing Grafana Cloud Prometheus/Mimir datasource UID; this module never creates it."
}

variable "cloudwatch_datasource_uid" {
  type        = string
  description = "Existing CloudWatch datasource UID; import it before enabling RDS alerts."
}

variable "slack_webhook_url" {
  type        = string
  sensitive   = true
  description = "Slack incoming-webhook URL. Inject through a secret-backed tfvars file or CI secret."
}

variable "aws_region" {
  type        = string
  default     = "ap-northeast-2"
  description = "AWS region containing the shared RDS instance."
}

variable "rds_instance_identifier" {
  type        = string
  description = "Existing shared RDS instance identifier."
}

variable "rds_max_connections" {
  type        = number
  description = "Observed/approved RDS max_connections value used for the 80% alert threshold."
}
