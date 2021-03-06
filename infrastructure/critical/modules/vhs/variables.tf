variable "name" {}

variable "bucket_name_prefix" {
  default = "wellcomecollection-vhs-"
}

variable "table_name_prefix" {
  default = "vhs-"
}

variable "aws_region" {
  default = "eu-west-1"
}

variable "read_principles" {
  default = []
  type    = list(string)
}
