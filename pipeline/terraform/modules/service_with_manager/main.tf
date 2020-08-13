module "worker" {
  source = "../../../../infrastructure/modules/worker_with_sidecar"

  name = var.service_name

  subnets = var.subnets

  cluster_name = var.cluster_name
  cluster_arn  = var.cluster_arn

  launch_type                  = var.launch_type
  capacity_provider_strategies = var.capacity_provider_strategies
  ordered_placement_strategies = var.ordered_placement_strategies

  desired_task_count = var.desired_task_count

  security_group_ids = var.security_group_ids

  app_name            = var.app_container_name
  app_image           = var.app_container_image
  app_cpu             = var.app_cpu
  app_memory          = var.app_memory
  app_env_vars        = var.app_env_vars
  app_secret_env_vars = var.app_secret_env_vars
  app_healthcheck     = var.app_healthcheck

  sidecar_image           = var.manager_container_image
  sidecar_name            = var.manager_container_name
  sidecar_cpu             = var.manager_cpu
  sidecar_memory          = var.manager_memory
  sidecar_env_vars        = var.manager_env_vars
  sidecar_secret_env_vars = var.manager_secret_env_vars

  cpu    = var.host_cpu
  memory = var.host_memory

  min_capacity = var.min_capacity
  max_capacity = var.max_capacity

  deployment_service_env  = var.deployment_service_env
  deployment_service_name = var.deployment_service_name
}
