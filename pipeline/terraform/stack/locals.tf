locals {
  namespace        = "catalogue-${var.pipeline_date}"
  namespace_hyphen = replace(local.namespace, "_", "-")

  es_works_source_index       = "works-source-${var.pipeline_date}"
  es_works_merged_index       = "works-merged-${var.pipeline_date}"
  es_works_identified_index   = "works-identified-${var.pipeline_date}"
  es_works_denormalised_index = "works-denormalised-${var.pipeline_date}"
  es_works_index              = "works-${var.pipeline_date}"

  es_images_initial_index   = "images-initial-${var.pipeline_date}"
  es_images_augmented_index = "images-augmented-${var.pipeline_date}"
  es_images_index           = "images-${var.pipeline_date}"

  # The max number of connections allowed by the instance.
  # specified at /infrastructure/critical/rds_id_minter.tf
  base_rds_instances             = 1
  id_minter_rds_max_connections  = (local.base_rds_instances + local.extra_rds_instances) * 45
  id_minter_task_max_connections = min(9, local.max_capacity)

  # We don't want to overload our databases if we're not reindexing
  # and don't have extra database capacity provisioned
  max_capacity = var.is_reindexing ? var.max_capacity : min(3, var.max_capacity)

  # If we're reindexing, our services will scale up to max capacity,
  # work through everything on the reindex queues, and then suddenly
  # finish processing everything -- at which point they all become idle.
  #
  # If we only stop one task per minute, that's a lot of tasks doing
  # nothing.  By increasing the scale_down_adjustment during reindexes,
  # we'll stop tasks faster and make reindexing cheaper.
  #
  # Note: if the scale down adjustment is greater than the number of tasks,
  # ECS will just stop every task.  e.g. if scale_down_adjustment = -5 and
  # there are 3 tasks running, ECS will scale the tasks down to zero.
  scale_down_adjustment = var.is_reindexing ? -5 : -1
  scale_up_adjustment   = 1

  services = [
    "ingestor_works",
    "ingestor_images",
    "matcher",
    "merger",
    "id_minter",
    "inference_manager",
    "feature_inferrer",
    "feature_training",
    "palette_inferrer",
    "aspect_ratio_inferrer",
    "router",
    "batcher",
    "relation_embedder",
    "transformer_miro",
    "transformer_mets",
    "transformer_sierra",
    "transformer_calm",
  ]

  sierra_adapter_topic_arns = var.is_reindexing ? concat(var.adapters["sierra"].topics, [var.adapters["sierra"].reindex_topic]) : var.adapters["sierra"].topics
  miro_adapter_topic_arns   = var.is_reindexing ? concat(var.adapters["miro"].topics, [var.adapters["miro"].reindex_topic]) : var.adapters["miro"].topics
  mets_adapter_topic_arns   = var.is_reindexing ? concat(var.adapters["mets"].topics, [var.adapters["mets"].reindex_topic]) : var.adapters["mets"].topics
  calm_adapter_topic_arns   = var.is_reindexing ? concat(var.adapters["calm"].topics, [var.adapters["calm"].reindex_topic]) : var.adapters["calm"].topics
}

