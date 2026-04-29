variable "project_id" {
  description = "Google Cloud project that hosts the Cloud Run workshop platform."
  type        = string
}

variable "region" {
  description = "Google Cloud region for Cloud Run services, Artifact Registry, and session workspaces."
  type        = string
  default     = "europe-west4"
}

variable "environment" {
  description = "Environment name included in resource names and labels."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,16}$", var.environment))
    error_message = "environment must start with a lowercase letter, contain only lowercase letters, numbers, and hyphens, and stay short enough for service account ids."
  }
}

variable "gateway_host" {
  description = "Public gateway host that owns stable /session/{sessionId}/ routes."
  type        = string
}

variable "event_id" {
  description = "Event identifier applied to dynamic Cloud Run session service labels for cost attribution."
  type        = string
  default     = "unspecified"
}

variable "artifact_repository_id" {
  description = "Artifact Registry repository id for Cloud Run session runner images."
  type        = string
  default     = "session-runners"
}

variable "workspace_bucket_name" {
  description = "Optional globally unique Cloud Storage bucket name for session workspace snapshots."
  type        = string
  default     = null
}

variable "workspace_retention_days" {
  description = "Default lifecycle age for deleting retained session workspace objects."
  type        = number
  default     = 7
}

variable "billing_export_dataset_id" {
  description = "BigQuery dataset id used as the Cloud Billing export landing dataset."
  type        = string
  default     = "workshop_billing_export"
}

variable "billing_export_location" {
  description = "BigQuery location for the Cloud Billing export dataset."
  type        = string
  default     = "EU"
}

variable "billing_account_id" {
  description = "Optional billing account id. When set with a positive budget amount, Terraform creates a project scoped budget alert."
  type        = string
  default     = null
}

variable "budget_amount_units" {
  description = "Whole currency units for the optional project scoped budget alert. Set to 0 to disable budget creation."
  type        = number
  default     = 0
}

variable "budget_currency" {
  description = "Currency code for the optional budget alert."
  type        = string
  default     = "USD"
}

variable "budget_alert_thresholds" {
  description = "Budget alert thresholds as fractions of the configured budget amount."
  type        = set(number)
  default     = [0.5, 0.8, 1.0]
}

variable "budget_notification_channels" {
  description = "Monitoring notification channel ids that should receive optional budget alerts."
  type        = list(string)
  default     = []
}

variable "budget_disable_default_iam_recipients" {
  description = "Whether optional budget alerts should skip default billing IAM recipients."
  type        = bool
  default     = false
}

variable "control_plane_service_name" {
  description = "Cloud Run service name for the public control plane."
  type        = string
  default     = "workshop-control-plane"
}

variable "execution_plane_service_name" {
  description = "Cloud Run service name for the private execution plane API."
  type        = string
  default     = "workshop-execution-plane"
}

variable "execution_plane_shared_secret" {
  description = "Shared secret sent by the control plane to the execution plane internal API."
  type        = string
  sensitive   = true
}

variable "control_plane_image" {
  description = "Digest pinned control plane image."
  type        = string
  default     = "europe-west4-docker.pkg.dev/example/workshops/control-plane@sha256:0000000000000000000000000000000000000000000000000000000000000000"
}

variable "execution_plane_image" {
  description = "Digest pinned execution plane image."
  type        = string
  default     = "europe-west4-docker.pkg.dev/example/workshops/execution-plane@sha256:0000000000000000000000000000000000000000000000000000000000000000"
}

variable "control_plane_port" {
  description = "Container port exposed by the control plane image."
  type        = number
  default     = 8080
}

variable "execution_plane_port" {
  description = "Container port exposed by the execution plane image."
  type        = number
  default     = 8080
}

variable "control_plane_min_instances" {
  description = "Minimum always warm control plane instances."
  type        = number
  default     = 0
}

variable "control_plane_max_instances" {
  description = "Maximum control plane instances."
  type        = number
  default     = 10
}

variable "control_plane_readiness_poll_interval" {
  description = "Interval used by the control plane when checking a deployed session runner for readiness."
  type        = string
  default     = "15s"

  validation {
    condition     = can(regex("^[1-9][0-9]*(ms|s|m|h)$", var.control_plane_readiness_poll_interval))
    error_message = "control_plane_readiness_poll_interval must be a positive duration such as 15s."
  }
}

variable "control_plane_session_default_ttl" {
  description = "Default control plane session lifetime for non release backed sessions."
  type        = string
  default     = "180m"

  validation {
    condition     = can(regex("^[1-9][0-9]*(ms|s|m|h)$", var.control_plane_session_default_ttl))
    error_message = "control_plane_session_default_ttl must be a positive duration such as 180m."
  }
}

variable "execution_plane_min_instances" {
  description = "Minimum always warm execution plane instances."
  type        = number
  default     = 0
}

variable "execution_plane_max_instances" {
  description = "Maximum execution plane instances."
  type        = number
  default     = 5
}

variable "execution_plane_ingress" {
  description = "Ingress setting for the long lived execution plane API. The current control plane call path uses the Cloud Run URL, so keep all ingress until private service routing is implemented."
  type        = string
  default     = "INGRESS_TRAFFIC_ALL"
}

variable "session_runner_manager_port" {
  description = "Default manager port for dynamically created session services."
  type        = number
  default     = 8080
}

variable "session_runner_manager_restart_timeout" {
  description = "Timeout used by the execution plane while waiting for a session runner manager restart or rebuild restart."
  type        = string
  default     = "290s"

  validation {
    condition     = can(regex("^[1-9][0-9]*(ms|s|m|h)$", var.session_runner_manager_restart_timeout))
    error_message = "session_runner_manager_restart_timeout must be a positive duration such as 290s."
  }
}

variable "cloud_run_operation_poll_interval" {
  description = "Interval used by the execution plane when polling Cloud Run Admin API operations during service create, update, or delete."
  type        = string
  default     = "5s"

  validation {
    condition     = can(regex("^[1-9][0-9]*(ms|s|m|h)$", var.cloud_run_operation_poll_interval))
    error_message = "cloud_run_operation_poll_interval must be a positive duration such as 5s."
  }
}

variable "session_runner_concurrency" {
  description = "Default Cloud Run concurrency for session runner services."
  type        = number
  default     = 1000
}

variable "session_runner_min_instances" {
  description = "Default minimum instances for session runner services."
  type        = number
  default     = 0
}

variable "session_runner_max_instances" {
  description = "Default maximum instances for session runner services."
  type        = number
  default     = 1
}

variable "session_runner_ingress" {
  description = "Default ingress setting for dynamically created session services. The current path proxy uses the Cloud Run URL, so keep all ingress until private service routing is implemented."
  type        = string
  default     = "INGRESS_TRAFFIC_ALL"
}

variable "labels" {
  description = "Additional labels applied to managed resources."
  type        = map(string)
  default     = {}
}

variable "session_runner_secret_ids" {
  description = "Secret Manager secret ids the session runner service account may read."
  type        = set(string)
  default     = []
}
