module "catalogue_pipeline_2021-06-08" {
  source = "./stack"

  pipeline_date = "2021-06-08"
  release_label = "2021-06-08"

  is_reindexing = false

  # Boilerplate that shouldn't change between pipelines.

  adapters = {
    sierra = {
      topics = [
        local.sierra_merged_bibs_topic_arn,
        local.sierra_merged_items_topic_arn,
        local.sierra_merged_holdings_topic_arn,
      ]
      reindex_topic = local.sierra_reindexer_topic_arn
    }

    miro = {
      topics = [
        local.miro_updates_topic_arn
      ]
      reindex_topic = local.miro_reindexer_topic_arn,
    }

    mets = {
      topics = [
        local.mets_adapter_topic_arn,
      ],
      reindex_topic = local.mets_reindexer_topic_arn,
    }

    calm = {
      topics = [
        local.calm_adapter_topic_arn,
        local.calm_deletions_topic_arn,
      ],
      reindex_topic = local.calm_reindexer_topic_arn,
    }
  }

  aws_region = local.aws_region
  vpc_id     = local.vpc_id
  subnets    = local.private_subnets

  dlq_alarm_arn = local.dlq_alarm_arn

  rds_cluster_id        = local.rds_cluster_id
  rds_subnet_group_name = local.rds_subnet_group_name

  # Security groups
  rds_ids_access_security_group_id = local.rds_access_security_group_id
  ec_privatelink_security_group_id = local.ec_platform_privatelink_security_group_id

  traffic_filter_platform_vpce_id   = local.traffic_filter_platform_vpce_id
  traffic_filter_catalogue_vpce_id  = local.traffic_filter_catalogue_vpce_id
  traffic_filter_public_internet_id = local.traffic_filter_public_internet_id

  # Adapter VHS
  vhs_miro_read_policy   = local.vhs_miro_read_policy
  vhs_sierra_read_policy = local.vhs_sierra_read_policy
  vhs_calm_read_policy   = local.vhs_calm_read_policy

  # Inferrer data
  inferrer_model_data_bucket_name = aws_s3_bucket.inferrer_model_core_data.id

  shared_logging_secrets = data.terraform_remote_state.shared_infra.outputs.shared_secrets_logging

  storage_bucket_name = local.storage_bucket

  providers = {
    aws.catalogue = aws.catalogue
  }

  logging_cluster_id = local.logging_cluster_id
}

module "catalogue_pipeline_2021-06-27" {
  source = "./stack"

  pipeline_date = "2021-06-27"
  release_label = "2021-06-27"

  is_reindexing = false

  # Boilerplate that shouldn't change between pipelines.

  adapters = {
    sierra = {
      topics = [
        local.sierra_merged_bibs_topic_arn,
        local.sierra_merged_items_topic_arn,
        local.sierra_merged_holdings_topic_arn,
      ]
      reindex_topic = local.sierra_reindexer_topic_arn
    }

    miro = {
      topics = [
        local.miro_updates_topic_arn,
      ]
      reindex_topic = local.miro_reindexer_topic_arn,
    }

    mets = {
      topics = [
        local.mets_adapter_topic_arn,
      ],
      reindex_topic = local.mets_reindexer_topic_arn,
    }

    calm = {
      topics = [
        local.calm_adapter_topic_arn,
        local.calm_deletions_topic_arn,
      ],
      reindex_topic = local.calm_reindexer_topic_arn,
    }
  }

  aws_region = local.aws_region
  vpc_id     = local.vpc_id
  subnets    = local.private_subnets

  dlq_alarm_arn = local.dlq_alarm_arn

  rds_cluster_id        = local.rds_cluster_id
  rds_subnet_group_name = local.rds_subnet_group_name

  # Security groups
  rds_ids_access_security_group_id = local.rds_access_security_group_id
  ec_privatelink_security_group_id = local.ec_platform_privatelink_security_group_id

  traffic_filter_platform_vpce_id   = local.traffic_filter_platform_vpce_id
  traffic_filter_catalogue_vpce_id  = local.traffic_filter_catalogue_vpce_id
  traffic_filter_public_internet_id = local.traffic_filter_public_internet_id

  # Adapter VHS
  vhs_miro_read_policy   = local.vhs_miro_read_policy
  vhs_sierra_read_policy = local.vhs_sierra_read_policy
  vhs_calm_read_policy   = local.vhs_calm_read_policy

  # Inferrer data
  inferrer_model_data_bucket_name = aws_s3_bucket.inferrer_model_core_data.id

  shared_logging_secrets = data.terraform_remote_state.shared_infra.outputs.shared_secrets_logging

  storage_bucket_name = local.storage_bucket

  providers = {
    aws.catalogue = aws.catalogue
  }

  logging_cluster_id = local.logging_cluster_id
}
