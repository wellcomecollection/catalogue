module "sierra_transformer_queue" {
  source          = "git::github.com/wellcomecollection/terraform-aws-sqs//queue?ref=v1.1.2"
  queue_name      = "${local.namespace_hyphen}_sierra_transformer"
  topic_arns      = var.sierra_adapter_topic_arns
  alarm_topic_arn = var.dlq_alarm_arn
  aws_region      = var.aws_region
}

module "sierra_transformer" {
  source          = "../modules/service"
  service_name    = "${local.namespace_hyphen}_sierra_transformer"
  container_image = local.transformer_sierra_image
  security_group_ids = [
    aws_security_group.service_egress.id,
    aws_security_group.interservice.id,
  ]

  cluster_name = aws_ecs_cluster.cluster.name
  cluster_arn  = aws_ecs_cluster.cluster.arn

  env_vars = {
    sns_arn                = module.sierra_transformer_topic.arn
    transformer_queue_id   = module.sierra_transformer_queue.url
    metrics_namespace      = "${local.namespace_hyphen}_sierra_transformer"
    messages_bucket_name   = aws_s3_bucket.messages.id
    vhs_sierra_bucket_name = var.vhs_sierra_sourcedata_bucket_name
    vhs_sierra_table_name  = var.vhs_sierra_sourcedata_table_name
    es_index              = local.es_works_source_index
  }

  secret_env_vars = {
    es_host     = "catalogue/pipeline_storage/es_host"
    es_port     = "catalogue/pipeline_storage/es_port"
    es_protocol = "catalogue/pipeline_storage/es_protocol"
    es_username = "catalogue/pipeline_storage/transformer/es_username"
    es_password = "catalogue/pipeline_storage/transformer/es_password"
  }

  subnets             = var.subnets
  max_capacity        = 10
  messages_bucket_arn = aws_s3_bucket.messages.arn

  queue_read_policy = module.sierra_transformer_queue.read_policy

  deployment_service_env  = var.release_label
  deployment_service_name = "sierra-transformer"
  shared_logging_secrets  = var.shared_logging_secrets
}

resource "aws_iam_role_policy" "sierra_transformer_vhs_sierra_adapter_read" {
  role   = module.sierra_transformer.task_role_name
  policy = var.vhs_sierra_read_policy
}

module "sierra_transformer_topic" {
  source = "../modules/topic"

  name       = "${local.namespace_hyphen}_sierra_transformer"
  role_names = [module.sierra_transformer.task_role_name]

  messages_bucket_arn = aws_s3_bucket.messages.arn
}

module "sierra_transformer_scaling_alarm" {
  source     = "git::github.com/wellcomecollection/terraform-aws-sqs//autoscaling?ref=v1.1.3"
  queue_name = module.sierra_transformer_queue.name

  queue_high_actions = [module.sierra_transformer.scale_up_arn]
  queue_low_actions  = [module.sierra_transformer.scale_down_arn]
}
