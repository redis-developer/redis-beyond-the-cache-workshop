locals {
  platform_service_accounts = {
    control_plane   = "${var.environment}-ws-cp"
    execution_plane = "${var.environment}-ws-ep"
    session_runner  = "${var.environment}-ws-runner"
  }

  required_services = toset([
    "artifactregistry.googleapis.com",
    "bigquery.googleapis.com",
    "billingbudgets.googleapis.com",
    "iam.googleapis.com",
    "logging.googleapis.com",
    "monitoring.googleapis.com",
    "run.googleapis.com",
    "secretmanager.googleapis.com",
    "storage.googleapis.com"
  ])

  workspace_bucket_name = coalesce(
    var.workspace_bucket_name,
    "${var.project_id}-${var.environment}-workshop-session-workspaces"
  )

  common_labels = merge(var.labels, {
    environment = var.environment
    managed_by  = "terraform"
    runtime     = "cloud-run"
  })
}

data "google_project" "current" {
  project_id = var.project_id
}

resource "google_project_service" "required" {
  for_each = local.required_services

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "session_runners" {
  project       = var.project_id
  location      = var.region
  repository_id = var.artifact_repository_id
  description   = "Digest pinned Cloud Run session runner images."
  format        = "DOCKER"

  labels = local.common_labels

  depends_on = [google_project_service.required]
}

resource "google_bigquery_dataset" "billing_export" {
  project    = var.project_id
  dataset_id = var.billing_export_dataset_id
  location   = var.billing_export_location

  friendly_name = "${var.environment} workshop billing export"
  description   = "Landing dataset for Cloud Billing export used by workshop cost attribution reports."
  labels        = local.common_labels

  delete_contents_on_destroy = false

  depends_on = [google_project_service.required]
}

resource "google_billing_budget" "cloud_run_event" {
  count = var.billing_account_id == null || var.budget_amount_units <= 0 ? 0 : 1

  billing_account = var.billing_account_id
  display_name    = "${var.environment} workshop Cloud Run event budget"

  budget_filter {
    projects = ["projects/${data.google_project.current.number}"]
  }

  amount {
    specified_amount {
      currency_code = var.budget_currency
      units         = tostring(var.budget_amount_units)
    }
  }

  dynamic "threshold_rules" {
    for_each = var.budget_alert_thresholds
    content {
      threshold_percent = threshold_rules.value
      spend_basis       = "CURRENT_SPEND"
    }
  }

  all_updates_rule {
    monitoring_notification_channels = var.budget_notification_channels
    disable_default_iam_recipients   = var.budget_disable_default_iam_recipients
  }

  depends_on = [google_project_service.required]
}

resource "google_service_account" "platform" {
  for_each = local.platform_service_accounts

  project      = var.project_id
  account_id   = each.value
  display_name = "Workshop ${replace(each.key, "_", " ")}"

  depends_on = [google_project_service.required]
}

resource "google_storage_bucket" "session_workspaces" {
  project  = var.project_id
  name     = local.workspace_bucket_name
  location = var.region

  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  labels = local.common_labels

  lifecycle_rule {
    action {
      type = "Delete"
    }
    condition {
      age            = var.workspace_retention_days
      matches_prefix = ["sessions/"]
    }
  }

  depends_on = [google_project_service.required]
}

resource "google_cloud_run_v2_service" "control_plane" {
  project              = var.project_id
  location             = var.region
  name                 = var.control_plane_service_name
  ingress              = "INGRESS_TRAFFIC_ALL"
  invoker_iam_disabled = true
  deletion_protection  = false

  labels = merge(local.common_labels, {
    component = "control-plane"
  })

  template {
    service_account = google_service_account.platform["control_plane"].email

    scaling {
      min_instance_count = var.control_plane_min_instances
      max_instance_count = var.control_plane_max_instances
    }

    containers {
      image = var.control_plane_image

      resources {
        cpu_idle = false
      }

      ports {
        container_port = var.control_plane_port
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "cloudrun"
      }

      env {
        name  = "EXECUTION_PLANE_BASE_URL"
        value = google_cloud_run_v2_service.execution_plane.uri
      }

      env {
        name  = "EXECUTION_PLANE_SHARED_SECRET"
        value = var.execution_plane_shared_secret
      }

      env {
        name  = "PLATFORM_CONTROLPLANE_EXECUTION_READINESS_POLL_INTERVAL"
        value = var.control_plane_readiness_poll_interval
      }

      env {
        name  = "PLATFORM_CONTROLPLANE_SESSION_DEFAULTS_TTL"
        value = var.control_plane_session_default_ttl
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [google_project_service.required]
}

resource "google_cloud_run_v2_service" "execution_plane" {
  project              = var.project_id
  location             = var.region
  name                 = var.execution_plane_service_name
  ingress              = var.execution_plane_ingress
  invoker_iam_disabled = true
  deletion_protection  = false

  labels = merge(local.common_labels, {
    component = "execution-plane"
  })

  template {
    service_account = google_service_account.platform["execution_plane"].email

    scaling {
      min_instance_count = var.execution_plane_min_instances
      max_instance_count = var.execution_plane_max_instances
    }

    containers {
      image = var.execution_plane_image

      ports {
        container_port = var.execution_plane_port
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_CLIENT"
        value = "google"
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_PROJECT_ID"
        value = var.project_id
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_REGION"
        value = var.region
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_GATEWAY_HOST"
        value = var.gateway_host
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_ENVIRONMENT"
        value = var.environment
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_EVENT_ID"
        value = var.event_id
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MANAGER_PORT"
        value = tostring(var.session_runner_manager_port)
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MANAGER_RESTART_TIMEOUT"
        value = var.session_runner_manager_restart_timeout
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_OPERATION_POLL_INTERVAL"
        value = var.cloud_run_operation_poll_interval
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_CONCURRENCY"
        value = tostring(var.session_runner_concurrency)
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MIN_INSTANCES"
        value = tostring(var.session_runner_min_instances)
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MAX_INSTANCES"
        value = tostring(var.session_runner_max_instances)
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_INGRESS"
        value = var.session_runner_ingress
      }

      env {
        name  = "PLATFORM_EXECUTION_PLANE_CLOUD_RUN_SERVICE_ACCOUNT"
        value = google_service_account.platform["session_runner"].email
      }

      env {
        name  = "WORKSHOP_SESSION_WORKSPACE_BUCKET"
        value = google_storage_bucket.session_workspaces.name
      }

      env {
        name  = "EXECUTION_PLANE_SHARED_SECRET"
        value = var.execution_plane_shared_secret
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [google_project_service.required]
}

resource "google_artifact_registry_repository_iam_member" "session_runner_pull" {
  project    = var.project_id
  location   = google_artifact_registry_repository.session_runners.location
  repository = google_artifact_registry_repository.session_runners.name
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.platform["session_runner"].email}"
}

resource "google_artifact_registry_repository_iam_member" "execution_plane_pull" {
  project    = var.project_id
  location   = google_artifact_registry_repository.session_runners.location
  repository = google_artifact_registry_repository.session_runners.name
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${google_service_account.platform["execution_plane"].email}"
}

resource "google_storage_bucket_iam_member" "session_runner_workspace_writer" {
  bucket = google_storage_bucket.session_workspaces.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.platform["session_runner"].email}"
}

resource "google_project_iam_member" "execution_plane_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.platform["execution_plane"].email}"
}

resource "google_service_account_iam_member" "execution_plane_uses_session_runner" {
  service_account_id = google_service_account.platform["session_runner"].name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.platform["execution_plane"].email}"
}

resource "google_project_iam_member" "platform_log_writers" {
  for_each = google_service_account.platform

  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${each.value.email}"
}

resource "google_project_iam_member" "platform_metric_writers" {
  for_each = google_service_account.platform

  project = var.project_id
  role    = "roles/monitoring.metricWriter"
  member  = "serviceAccount:${each.value.email}"
}

resource "google_secret_manager_secret_iam_member" "session_runner_secret_access" {
  for_each = var.session_runner_secret_ids

  project   = var.project_id
  secret_id = each.value
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.platform["session_runner"].email}"
}
