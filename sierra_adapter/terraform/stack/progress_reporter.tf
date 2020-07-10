module "progress_reporter" {
  source = "./../sierra_progress_reporter"

  trigger_interval_minutes = 900
  s3_adapter_bucket_name   = aws_s3_bucket.sierra_adapter.id
  slack_access_token       = local.critical_slack_webhook

  infra_bucket = var.infra_bucket

  lambda_error_alarm_arn = var.lambda_error_alarm_arn

  namespace = local.namespace_hyphen
}
