module "merger_queue" {
  source     = "git::github.com/wellcomecollection/terraform-aws-sqs//queue?ref=v1.1.2"
  queue_name = "${local.namespace_hyphen}_merger"
  topic_arns = [
  module.matcher_topic.arn]
  aws_region      = var.aws_region
  alarm_topic_arn = var.dlq_alarm_arn
}

module "merger" {
  source          = "../modules/service"
  service_name    = "${local.namespace_hyphen}_merger"
  container_image = local.merger_image
  security_group_ids = [
    aws_security_group.service_egress.id,
    aws_security_group.interservice.id,
  ]

  cluster_name = aws_ecs_cluster.cluster.name
  cluster_arn  = aws_ecs_cluster.cluster.arn

  env_vars = {
    metrics_namespace       = "${local.namespace_hyphen}_merger"
    messages_bucket_name    = aws_s3_bucket.messages.id
    topic_arn               = module.matcher_topic.arn
    merger_queue_id         = module.merger_queue.url
    merger_works_topic_arn  = module.merger_works_topic.arn
    merger_images_topic_arn = module.merger_images_topic.arn

    es_identified_works_index = local.es_works_identified_index
    es_merged_works_index     = local.es_works_merged_index

    batch_size             = 100
    flush_interval_seconds = 30
  }

  secret_env_vars = {
    es_host     = "catalogue/pipeline_storage/es_host"
    es_port     = "catalogue/pipeline_storage/es_port"
    es_protocol = "catalogue/pipeline_storage/es_protocol"
    es_username = "catalogue/pipeline_storage/merger/es_username"
    es_password = "catalogue/pipeline_storage/merger/es_password"
  }

  subnets             = var.subnets
  max_capacity        = 10
  messages_bucket_arn = aws_s3_bucket.messages.arn
  queue_read_policy   = module.merger_queue.read_policy

  deployment_service_env  = var.release_label
  deployment_service_name = "merger"
  shared_logging_secrets  = var.shared_logging_secrets
}

module "merger_works_topic" {
  source = "../modules/topic"

  name                = "${local.namespace_hyphen}_merger_works"
  role_names          = [module.merger.task_role_name]
  messages_bucket_arn = aws_s3_bucket.messages.arn
}

module "merger_images_topic" {
  source = "../modules/topic"

  name                = "${local.namespace_hyphen}_merger_images"
  role_names          = [module.merger.task_role_name]
  messages_bucket_arn = aws_s3_bucket.messages.arn
}

module "merger_scaling_alarm" {
  source     = "git::github.com/wellcomecollection/terraform-aws-sqs//autoscaling?ref=v1.1.3"
  queue_name = module.merger_queue.name

  queue_high_actions = [module.merger.scale_up_arn]
  queue_low_actions  = [module.merger.scale_down_arn]
}
