module "updates_queue" {
  source     = "git::https://github.com/wellcometrust/terraform.git//sqs?ref=v6.4.0"
  queue_name = "sierra_${var.resource_type}_merger_queue"
  aws_region = "${var.aws_region}"
  account_id = "${var.account_id}"

  topic_names = [
    "${var.updates_topic_name}",
    "${var.reindexed_items_topic_name}",
  ]

  topic_count = 2

  # Ensure that messages are spread around -- if the merger has an error
  # (for example, hitting DynamoDB write limits), we don't retry too quickly.
  visibility_timeout_seconds = 300

  # The bib merger queue has had consistent problems where the DLQ fills up,
  # and then redriving it fixes everything.  Increase the number of times a
  # message can be received before it gets marked as failed.
  max_receive_count = 12

  alarm_topic_arn = "${var.dlq_alarm_arn}"
}

module "scaling_alarm" {
  source     = "git::https://github.com/wellcometrust/terraform-modules.git//autoscaling/alarms/queue?ref=v19.12.0"
  queue_name = "sierra_${var.resource_type}_merger_queue"

  queue_high_actions = ["${module.sierra_merger_service.scale_up_arn}"]
  queue_low_actions  = ["${module.sierra_merger_service.scale_down_arn}"]
}