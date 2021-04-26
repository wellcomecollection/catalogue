variable "resource_type" {}

variable "bucket_name" {}
variable "windows_topic_arns" {}

variable "sierra_fields" {}

variable "sierra_api_url" {}

variable "container_image" {}

variable "cluster_name" {}
variable "cluster_arn" {}

variable "dlq_alarm_arn" {}
variable "lambda_error_alarm_arn" {}

variable "aws_region" {
  default = "eu-west-1"
}

variable "infra_bucket" {}

variable "subnets" {
  type = list(string)
}

variable "namespace_id" {}
variable "namespace" {}
variable "interservice_security_group_id" {}
variable "service_egress_security_group_id" {}

variable "deployment_service_env" {}
variable "deployment_service_name" {}
variable "shared_logging_secrets" {
  type = map(any)
}

variable "elastic_cloud_vpce_sg_id" {
  type = string
}
