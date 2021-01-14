resource "ec_deployment" "pipeline_storage" {
  provider = ec

  name = "catalogue-pipeline-storage-tf-managed"

  region                 = "eu-west-1"
  version                = "7.10.2"
  deployment_template_id = "aws-io-optimized-v2"

  traffic_filter = [
    ec_deployment_traffic_filter.allow_catalogue_pipeline_vpce.id
  ]

  elasticsearch {
    topology {
      zone_count = 2
      size       = "15g"
    }
  }

  kibana {
    topology {
      zone_count = 1
      size       = "1g"
    }
  }
}

resource "aws_security_group" "allow_catalogue_pipeline_elastic_cloud_vpce" {
  provider = aws

  name   = "allow_elastic_cloud_vpce"
  vpc_id = local.vpc_id_new

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

}

resource "aws_vpc_endpoint" "catalogue_pipeline_elastic_cloud_vpce" {
  provider = aws

  vpc_id            = local.vpc_id_new
  service_name      = local.ec_eu_west_1_service_name
  vpc_endpoint_type = "Interface"

  security_group_ids = [
    aws_security_group.allow_catalogue_pipeline_elastic_cloud_vpce.id,
  ]

  subnet_ids = local.private_subnets_new

  private_dns_enabled = false
}

resource "ec_deployment_traffic_filter" "allow_catalogue_pipeline_vpce" {
  provider = ec

  name   = "ec_allow_vpc_endpoint"
  region = "eu-west-1"
  type   = "vpce"

  rule {
    source = aws_vpc_endpoint.catalogue_pipeline_elastic_cloud_vpce.id
  }
}

resource "aws_route53_zone" "catalogue_pipeline_elastic_cloud_vpce" {
  name = local.catalogue_pipeline_ec_vpce_domain

  vpc {
    vpc_id = local.vpc_id_new
  }
}

resource "aws_route53_record" "cname_ec" {
  zone_id = aws_route53_zone.catalogue_pipeline_elastic_cloud_vpce.zone_id
  name    = "*.vpce.eu-west-1.aws.elastic-cloud.com"
  type    = "CNAME"
  ttl     = "60"
  records = [aws_vpc_endpoint.catalogue_pipeline_elastic_cloud_vpce.dns_entry[0]["dns_name"]]
}