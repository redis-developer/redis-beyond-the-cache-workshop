-- Cloud Run billing export cost grouped by the labels used for workshop attribution.
-- Replace {{BILLING_EXPORT_TABLE}} with project.dataset.table or use scripts/ops/cloud-run-cost-report.sh.
-- Query parameters:
--   @start_date DATE inclusive
--   @end_date   DATE exclusive

WITH labeled_costs AS (
  SELECT
    invoice.month AS invoice_month,
    DATE(usage_start_time) AS usage_date,
    service.description AS service_description,
    sku.description AS sku_description,
    COALESCE((SELECT value FROM UNNEST(labels) WHERE key = 'environment'), 'unlabeled') AS environment,
    COALESCE(
      (SELECT value FROM UNNEST(labels) WHERE key = 'event-id'),
      (SELECT value FROM UNNEST(labels) WHERE key = 'event_id'),
      'unlabeled'
    ) AS event_id,
    COALESCE((SELECT value FROM UNNEST(labels) WHERE key = 'runtime'), 'unlabeled') AS runtime,
    COALESCE((SELECT value FROM UNNEST(labels) WHERE key = 'component'), 'unlabeled') AS component,
    COALESCE(
      (SELECT value FROM UNNEST(labels) WHERE key = 'workshop-id'),
      (SELECT value FROM UNNEST(labels) WHERE key = 'workshop_id'),
      'unlabeled'
    ) AS workshop_id,
    COALESCE(
      (SELECT value FROM UNNEST(labels) WHERE key = 'workshop-session-id'),
      (SELECT value FROM UNNEST(labels) WHERE key = 'session_id'),
      'unlabeled'
    ) AS session_id,
    COALESCE(
      (SELECT value FROM UNNEST(labels) WHERE key = 'release-id'),
      (SELECT value FROM UNNEST(labels) WHERE key = 'release_id'),
      'unlabeled'
    ) AS release_id,
    COALESCE(
      (SELECT value FROM UNNEST(labels) WHERE key = 'resource-class'),
      (SELECT value FROM UNNEST(labels) WHERE key = 'resource_class'),
      'unlabeled'
    ) AS resource_class,
    COALESCE(
      (SELECT value FROM UNNEST(labels) WHERE key = 'redis-mode'),
      (SELECT value FROM UNNEST(labels) WHERE key = 'redis_mode'),
      'unlabeled'
    ) AS redis_mode,
    COALESCE(
      (SELECT value FROM UNNEST(labels) WHERE key = 'cost-owner'),
      (SELECT value FROM UNNEST(labels) WHERE key = 'cost_owner'),
      'unlabeled'
    ) AS cost_owner,
    cost,
    IFNULL((SELECT SUM(c.amount) FROM UNNEST(credits) AS c), 0) AS credits
  FROM `{{BILLING_EXPORT_TABLE}}`
  WHERE DATE(usage_start_time) >= @start_date
    AND DATE(usage_start_time) < @end_date
    AND service.description IN (
      'Cloud Run',
      'Artifact Registry',
      'Cloud Storage',
      'Secret Manager',
      'Cloud Logging',
      'Cloud Monitoring'
    )
)
SELECT
  invoice_month,
  usage_date,
  service_description,
  sku_description,
  environment,
  event_id,
  runtime,
  component,
  workshop_id,
  session_id,
  release_id,
  resource_class,
  redis_mode,
  cost_owner,
  ROUND(SUM(cost), 6) AS gross_cost,
  ROUND(SUM(credits), 6) AS credits,
  ROUND(SUM(cost) + SUM(credits), 6) AS net_cost
FROM labeled_costs
GROUP BY
  invoice_month,
  usage_date,
  service_description,
  sku_description,
  environment,
  event_id,
  runtime,
  component,
  workshop_id,
  session_id,
  release_id,
  resource_class,
  redis_mode,
  cost_owner
ORDER BY
  usage_date,
  service_description,
  event_id,
  component,
  workshop_id,
  session_id;
