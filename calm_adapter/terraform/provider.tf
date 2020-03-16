provider "aws" {
  region  = local.aws_region
  version = "~> 2.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-developer"
  }
}

provider "template" {
  version = "~> 2.1"
}
