module "catalogue_api_prod" {
  source = "../modules/catalogue_api_stack"

  environment        = "prod"
  domain_name        = "catalogue.api.wellcomecollection.org"
  listener_port      = 80
  task_desired_count = 3

  namespace    = "${local.namespace}"
  vpc_id       = "${local.vpc_id}"
  subnets      = ["${local.private_subnets}"]
  cluster_name = "${local.cluster_name}"
  api_id       = "${local.api_gateway_id}"

  lb_arn           = "${local.nlb_arn}"
  lb_ingress_sg_id = "${local.service_lb_ingress_security_group_id}"

  logstash_host = "${local.logstash_host}"

  interservice_sg_id = "${local.interservice_security_group_id}"

  certificate_arn = "${local.certificate_arn}"
  route53_zone_id = "${local.route53_zone_id}"

  api_gateway_id = "${local.api_gateway_id}"

  providers = {
    aws.platform    = "aws.platform"
    aws.routemaster = "aws.routemaster"
  }
}
