module "holdings_reader" {
  source = "./../sierra_reader"

  resource_type = "holdings"

  bucket_name        = aws_s3_bucket.sierra_adapter.id
  windows_topic_arns = var.holdings_windows_topic_arns

  sierra_fields = local.sierra_holdings_fields

  sierra_api_url = local.sierra_api_url

  container_image = local.sierra_reader_image

  cluster_name = aws_ecs_cluster.cluster.name
  cluster_arn  = aws_ecs_cluster.cluster.arn

  dlq_alarm_arn          = var.dlq_alarm_arn
  lambda_error_alarm_arn = var.lambda_error_alarm_arn

  infra_bucket = var.infra_bucket

  namespace_id = aws_service_discovery_private_dns_namespace.namespace.id
  namespace    = local.namespace_hyphen
  subnets      = var.private_subnets

  service_egress_security_group_id = var.egress_security_group_id
  interservice_security_group_id   = var.interservice_security_group_id
  elastic_cloud_vpce_sg_id         = var.elastic_cloud_vpce_sg_id

  deployment_service_env  = var.deployment_env
  deployment_service_name = "holdings-reader"
  shared_logging_secrets  = var.shared_logging_secrets
}

module "holdings_linker" {
  source = "./../sierra_linker"

  resource_type = "holdings"

  demultiplexer_topic_arn = module.holdings_reader.topic_arn

  container_image = local.sierra_linker_image

  cluster_name = aws_ecs_cluster.cluster.name
  cluster_arn  = aws_ecs_cluster.cluster.arn

  dlq_alarm_arn = var.dlq_alarm_arn

  namespace_id = aws_service_discovery_private_dns_namespace.namespace.id
  namespace    = local.namespace_hyphen
  subnets      = var.private_subnets

  service_egress_security_group_id = var.egress_security_group_id
  interservice_security_group_id   = var.interservice_security_group_id
  elastic_cloud_vpce_sg_id         = var.elastic_cloud_vpce_sg_id

  deployment_service_env  = var.deployment_env
  deployment_service_name = "holdings-linker"
  shared_logging_secrets  = var.shared_logging_secrets
}

module "holdings_merger" {
  source = "./../sierra_merger"

  resource_type     = "holdings"
  updates_topic_arn = module.holdings_linker.topic_arn

  container_image = local.sierra_merger_image

  vhs_table_name        = module.vhs_sierra.table_name
  vhs_bucket_name       = module.vhs_sierra.bucket_name
  vhs_read_write_policy = module.vhs_sierra.full_access_policy

  cluster_name = aws_ecs_cluster.cluster.name
  cluster_arn  = aws_ecs_cluster.cluster.arn

  dlq_alarm_arn = var.dlq_alarm_arn

  namespace_id = aws_service_discovery_private_dns_namespace.namespace.id
  namespace    = local.namespace_hyphen
  subnets      = var.private_subnets

  service_egress_security_group_id = var.egress_security_group_id
  interservice_security_group_id   = var.interservice_security_group_id
  elastic_cloud_vpce_sg_id         = var.elastic_cloud_vpce_sg_id

  deployment_service_env  = var.deployment_env
  deployment_service_name = "holdings-merger"
  shared_logging_secrets  = var.shared_logging_secrets
}
