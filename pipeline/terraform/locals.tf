locals {
  miro_updates_topic_arn = data.terraform_remote_state.shared_infra.outputs.miro_updates_topic_arn
  vhs_miro_read_policy   = data.terraform_remote_state.catalogue_infra_critical.outputs.vhs_miro_read_policy
  storage_bucket         = "wellcomecollection-storage"

  # Sierra adapter VHS
  vhs_sierra_read_policy = data.terraform_remote_state.sierra_adapter.outputs.vhs_read_policy

  # Sierra adapter topics
  sierra_merged_items_topic_arn = data.terraform_remote_state.sierra_adapter.outputs.merged_items_topic_arn
  sierra_merged_bibs_topic_arn  = data.terraform_remote_state.sierra_adapter.outputs.merged_bibs_topic_arn

  # Mets adapter VHS
  mets_adapter_read_policy = data.terraform_remote_state.mets_adapter.outputs.mets_dynamo_read_policy

  # Mets adapter topics
  mets_adapter_topic_arn = data.terraform_remote_state.mets_adapter.outputs.mets_adapter_topic_arn

  # Calm adapter VHS
  vhs_calm_read_policy = data.terraform_remote_state.calm_adapter.outputs.vhs_read_policy

  # Calm adapter topics
  calm_adapter_topic_arn = data.terraform_remote_state.calm_adapter.outputs.calm_adapter_topic_arn

  # Reindexer topics
  miro_reindexer_topic_arn   = data.terraform_remote_state.shared_infra.outputs.catalogue_miro_reindex_topic_arn
  sierra_reindexer_topic_arn = data.terraform_remote_state.shared_infra.outputs.catalogue_sierra_reindex_topic_arn
  mets_reindexer_topic_arn   = data.terraform_remote_state.reindexer.outputs.mets_reindexer_topic_arn
  calm_reindexer_topic_arn   = data.terraform_remote_state.reindexer.outputs.calm_reindexer_topic_arn

  # Infra stuff
  aws_region                   = "eu-west-1"
  dlq_alarm_arn                = data.terraform_remote_state.shared_infra.outputs.dlq_alarm_arn
  vpc_id                       = local.catalogue_vpcs["catalogue_vpc_delta_id"]
  private_subnets              = local.catalogue_vpcs["catalogue_vpc_delta_private_subnets"]
  rds_access_security_group_id = data.terraform_remote_state.catalogue_infra_critical.outputs.rds_access_security_group_id
  rds_cluster_id               = data.terraform_remote_state.catalogue_infra_critical.outputs.rds_cluster_id
  rds_subnet_group_name        = data.terraform_remote_state.catalogue_infra_critical.outputs.rds_subnet_group_name

  pipeline_storage_security_group_id = data.terraform_remote_state.catalogue_infra_critical.outputs["catalogue_pipeline_storage_elastic_cloud_sg_id"]
}
