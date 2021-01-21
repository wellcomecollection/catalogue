module "sierra_merger_service" {
  source = "../../../infrastructure/modules/worker"

  name = local.service_name

  deployment_service_env  = var.deployment_service_env
  deployment_service_name = var.deployment_service_name

  image = var.container_image

  env_vars = {
    windows_queue_url            = module.updates_queue.url
    metrics_namespace            = local.service_name
    sierra_vhs_dynamo_table_name = var.sierra_transformable_vhs_dynamo_table_name
    sierra_vhs_bucket_name       = var.sierra_transformable_vhs_bucket_name
    topic_arn                    = module.sierra_item_merger_results.arn

    # The item merger has to write lots of S3 objects, and we've seen issues
    # where we exhaust the HTTP connection pool.  Turning down the parallelism
    # is an attempt to reduce the number of S3 objects in flight, and avoid
    # these errors.
    sqs_parallelism = 5
  }

  min_capacity = 0
  max_capacity = 3

  use_fargate_spot = true

  namespace_id = var.namespace_id

  cluster_name = var.cluster_name
  cluster_arn  = var.cluster_arn

  subnets = var.subnets

  security_group_ids = [
    var.interservice_security_group_id,
    var.service_egress_security_group_id,
  ]
  shared_logging_secrets = var.shared_logging_secrets
}
