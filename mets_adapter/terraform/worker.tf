module "worker" {
  source = "../../infrastructure/modules/worker"

  name  = local.namespace
  image = local.mets_adapter_image

  env_vars = {
    sns_arn                   = module.mets_adapter_output_topic.arn
    queue_id                  = module.queue.url
    metrics_namespace         = local.namespace
    mets_adapter_dynamo_table = local.mets_adapter_table_name
    bag_api_url               = local.bag_api_url
    oauth_url                 = local.oauth_url
  }
  secret_env_vars = {
    oauth_client_id = "mets_adapter/mets_adapter/client_id"
    oauth_secret    = "mets_adapter/mets_adapter/secret"
  }

  min_capacity       = 0
  max_capacity       = 10
  desired_task_count = 0

  cluster_name           = aws_ecs_cluster.cluster.name
  cluster_arn            = aws_ecs_cluster.cluster.arn
  namespace_id           = aws_service_discovery_private_dns_namespace.namespace.id
  subnets                = local.private_subnets
  shared_logging_secrets = data.terraform_remote_state.shared_infra.outputs.shared_secrets_logging

  elastic_cloud_vpce_sg_id = data.terraform_remote_state.shared_infra.outputs.ec_platform_privatelink_sg_id
}
