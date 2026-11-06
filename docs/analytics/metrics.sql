-- BigQuery Standard SQL. Replace YOUR_PROJECT.analytics_PROPERTY_ID.
-- Daily export works with BigQuery Sandbox; no streaming export is required.
-- Parameters are random IDs/enums/numeric measurements only.
WITH events AS (
 SELECT event_name, event_timestamp,
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key='form_id') form_id,
  COALESCE((SELECT value.string_value FROM UNNEST(event_params) WHERE key='creation_attempt_id'),
           (SELECT value.string_value FROM UNNEST(event_params) WHERE key='attempt_id')) attempt_id,
  COALESCE((SELECT value.string_value FROM UNNEST(event_params) WHERE key='template_type'),
           (SELECT value.string_value FROM UNNEST(event_params) WHERE key='form_type')) template_type,
  COALESCE((SELECT value.double_value FROM UNNEST(event_params) WHERE key='duration_seconds'),
           CAST((SELECT value.int_value FROM UNNEST(event_params) WHERE key='duration_seconds') AS FLOAT64)) duration_seconds,
  (SELECT value.int_value FROM UNNEST(event_params) WHERE key='field_count') field_count
 FROM `YOUR_PROJECT.analytics_PROPERTY_ID.events_*`
 WHERE _TABLE_SUFFIX BETWEEN @start_date AND @end_date
 AND event_name IN ('form_creation_started','form_published','form_opened','form_submitted')
 AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key='environment')='users'
), starts AS (
 SELECT *, IF(event_name='form_creation_started','creation','submission') flow
 FROM events WHERE event_name IN ('form_creation_started','form_opened')
 QUALIFY ROW_NUMBER() OVER(PARTITION BY event_name,form_id,attempt_id ORDER BY event_timestamp)=1
), ends AS (
 SELECT *, IF(event_name='form_published','creation','submission') flow
 FROM events WHERE event_name IN ('form_published','form_submitted')
 QUALIFY ROW_NUMBER() OVER(PARTITION BY event_name,form_id,attempt_id ORDER BY event_timestamp)=1
), attempts AS (
 SELECT s.flow,s.form_id,s.attempt_id,COALESCE(e.template_type,s.template_type) template_type,e.field_count,
  e.event_timestamp IS NOT NULL completed,
  IF(e.duration_seconds>=0 AND e.event_timestamp>=s.event_timestamp,e.duration_seconds,NULL) valid_duration
 FROM starts s LEFT JOIN ends e USING(flow,form_id,attempt_id)
)
SELECT flow,template_type,field_count,
 COUNT(*) started_attempts,COUNTIF(completed) completed_attempts,
 COUNTIF(NOT completed) incomplete_at_cutoff,
 COUNTIF(completed AND valid_duration IS NULL) invalid_completed_measurements,
 COUNT(valid_duration) measured_completions,
 SAFE_DIVIDE(SUM(valid_duration),COUNT(valid_duration)) average_duration_seconds,
 IF(flow='creation',SAFE_DIVIDE(SUM(valid_duration),COUNT(valid_duration))<600,NULL) creation_target_under_600s
FROM attempts GROUP BY flow,template_type,field_count;
-- Use a reporting range including starts from before the completion window when attempts span days.
-- Incomplete at cutoff is not an assertion that a person permanently abandoned the flow.
