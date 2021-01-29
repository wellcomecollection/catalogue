resource "ec_deployment" "catalogue" {
  name = "catalogue"

  region                 = "eu-west-1"
  version                = "7.9.2"
  deployment_template_id = "aws-io-optimized"

  traffic_filter = [
    ec_deployment_traffic_filter.public_internet.id,
    module.platform_privatelink.traffic_filter_vpce_id,
    module.catalogue_privatelink.traffic_filter_vpce_id,
  ]

  elasticsearch {
    ref_id = "elasticsearch"

    topology {
      zone_count = 3
      size       = "8g"
    }
  }

  kibana {
    elasticsearch_cluster_ref_id = "elasticsearch"
    ref_id                       = "kibana"

    topology {
      zone_count = 1
      size       = "1g"
    }
  }
}

locals {
  catalogue_elastic_id     = ec_deployment.catalogue.elasticsearch[0].resource_id
  catalogue_elastic_region = ec_deployment.catalogue.elasticsearch[0].region
}

module "catalogue_secrets_platform" {
  source = "../modules/secrets"

  key_value_map = {
    "elasticsearch/catalogue/public_host" = "${local.catalogue_elastic_id}.${local.catalogue_elastic_region}.aws.found.io"

    # See https://www.elastic.co/guide/en/cloud/current/ec-traffic-filtering-vpc.html
    "elasticsearch/catalogue/private_host" = "${local.catalogue_elastic_id}.vpce.${local.catalogue_elastic_region}.aws.elastic-cloud.com"
  }
}

module "catalogue_secrets" {
  source = "../modules/secrets"

  providers = {
    aws = aws.catalogue
  }

  key_value_map = {
    "elasticsearch/catalogue/public_host" = "${local.catalogue_elastic_id}.${local.catalogue_elastic_region}.aws.found.io"

    # See https://www.elastic.co/guide/en/cloud/current/ec-traffic-filtering-vpc.html
    "elasticsearch/catalogue/private_host" = "${local.catalogue_elastic_id}.vpce.${local.catalogue_elastic_region}.aws.elastic-cloud.com"
  }
}