output "artifact_registry_repository" {
  description = "Artifact Registry repository for Cloud Run session runner images."
  value = {
    location      = google_artifact_registry_repository.session_runners.location
    repository_id = google_artifact_registry_repository.session_runners.repository_id
    repository    = "${google_artifact_registry_repository.session_runners.location}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.session_runners.repository_id}"
  }
}

output "cloud_run_services" {
  description = "Long lived Cloud Run platform services. Per session services are created dynamically by the execution plane."
  value = {
    control_plane = {
      name = google_cloud_run_v2_service.control_plane.name
      uri  = google_cloud_run_v2_service.control_plane.uri
    }
    execution_plane = {
      name = google_cloud_run_v2_service.execution_plane.name
      uri  = google_cloud_run_v2_service.execution_plane.uri
    }
  }
}

output "service_accounts" {
  description = "Google service accounts used by the Cloud Run platform."
  value = {
    for key, account in google_service_account.platform : key => account.email
  }
}

output "workspace_bucket" {
  description = "Cloud Storage bucket used for durable session workspace snapshots."
  value = {
    name             = google_storage_bucket.session_workspaces.name
    url              = google_storage_bucket.session_workspaces.url
    retention_days   = var.workspace_retention_days
    workspace_prefix = "sessions"
  }
}

output "cost_tracking" {
  description = "Cost tracking resources and labels used for Cloud Run session attribution."
  value = {
    billing_export_dataset = {
      project    = google_bigquery_dataset.billing_export.project
      dataset_id = google_bigquery_dataset.billing_export.dataset_id
      location   = google_bigquery_dataset.billing_export.location
    }
    budget_name = var.billing_account_id == null || var.budget_amount_units <= 0 ? null : google_billing_budget.cloud_run_event[0].name
    session_service_labels = [
      "environment",
      "event-id",
      "workshop-id",
      "workshop-session-id",
      "release-id",
      "resource-class",
      "redis-mode",
      "managed-by"
    ]
  }
}

output "session_runner_defaults" {
  description = "Environment defaults consumed by the execution plane when it creates one Cloud Run service per learner session."
  value = {
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_CLIENT                  = "google"
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_PROJECT_ID              = var.project_id
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_REGION                  = var.region
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_GATEWAY_HOST            = var.gateway_host
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_ENVIRONMENT             = var.environment
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_EVENT_ID                = var.event_id
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MANAGER_PORT            = tostring(var.session_runner_manager_port)
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MANAGER_RESTART_TIMEOUT = var.session_runner_manager_restart_timeout
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_OPERATION_POLL_INTERVAL = var.cloud_run_operation_poll_interval
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_CONCURRENCY             = tostring(var.session_runner_concurrency)
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MIN_INSTANCES           = tostring(var.session_runner_min_instances)
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MAX_INSTANCES           = tostring(var.session_runner_max_instances)
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_INGRESS                 = var.session_runner_ingress
    PLATFORM_EXECUTION_PLANE_CLOUD_RUN_SERVICE_ACCOUNT         = google_service_account.platform["session_runner"].email
    WORKSHOP_SESSION_WORKSPACE_BUCKET                          = google_storage_bucket.session_workspaces.name
    WORKSHOP_SESSION_WORKSPACE_PREFIX                          = "sessions"
    WORKSHOP_SESSION_RUNNER_ARTIFACT_REPOSITORY                = google_artifact_registry_repository.session_runners.repository_id
  }
}
