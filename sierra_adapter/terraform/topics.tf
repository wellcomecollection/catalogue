module "items_reharvest_topic" {
  source = "github.com/wellcomecollection/terraform-aws-sns-topic.git?ref=v1.0.0"
  name   = "sierra_items_reharvest_windows"
}
module "bibs_reharvest_topic" {
  source = "github.com/wellcomecollection/terraform-aws-sns-topic.git?ref=v1.0.0"
  name   = "sierra_bibs_reharvest_windows"
}
module "holdings_reharvest_topic" {
  source = "github.com/wellcomecollection/terraform-aws-sns-topic.git?ref=v1.0.0"
  name   = "sierra_holdings_reharvest_windows"
}

module "orders_reharvest_topic" {
  source = "github.com/wellcomecollection/terraform-aws-sns-topic.git?ref=v1.0.0"
  name   = "sierra_orders_reharvest_windows"
}
