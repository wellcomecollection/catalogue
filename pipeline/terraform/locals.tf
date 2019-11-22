locals {
  miro_updates_topic_name = "${data.terraform_remote_state.shared_infra.miro_updates_topic_name}"
  vhs_miro_read_policy = "${data.terraform_remote_state.catalogue_infra_critical.vhs_miro_read_policy}"

  # Sierra adapter VHS
  vhs_sierra_read_policy = "${data.terraform_remote_state.catalogue_infra_critical.vhs_sierra_read_policy}"
  vhs_sierra_sourcedata_bucket_name = "${data.terraform_remote_state.catalogue_infra_critical.vhs_sierra_bucket_name}"
  vhs_sierra_sourcedata_table_name  = "${data.terraform_remote_state.catalogue_infra_critical.vhs_sierra_table_name}"

  # Sierra adapter topics
  sierra_merged_items_topic_name = "${data.terraform_remote_state.sierra_adapter.merged_items_topic_name}"
  sierra_merged_bibs_topic_name  = "${data.terraform_remote_state.sierra_adapter.merged_bibs_topic_name}"

  # Mets adapter VHS
  vhs_mets_adapter_read_policy = "${data.terraform_remote_state.catalogue_infra_critical.vhs_mets_read_policy}"
  vhs_mets_adapter_bucket_name = "${data.terraform_remote_state.catalogue_infra_critical.vhs_mets_bucket_name}"
  vhs_mets_adapter_table_name  = "${data.terraform_remote_state.catalogue_infra_critical.vhs_mets_table_name}"

  # Mets adapter topics
  mets_vhs_keys_topic_name = "${data.terraform_remote_state.mets_adapter.mets_vhs_keys_topic_name}"

  # Reindexer topics
  miro_reindexer_topic_name   = "${data.terraform_remote_state.shared_infra.catalogue_miro_reindex_topic_name}"
  sierra_reindexer_topic_name = "${data.terraform_remote_state.shared_infra.catalogue_sierra_reindex_topic_name}"

  # Infra stuff
  infra_bucket = "${data.terraform_remote_state.shared_infra.infra_bucket}"
  aws_region = "eu-west-1"
  dlq_alarm_arn = "${data.terraform_remote_state.shared_infra.dlq_alarm_arn}"
  vpc_id = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"
  rds_access_security_group_id = "${data.terraform_remote_state.catalogue_infra_critical.rds_access_security_group_id}"
}
