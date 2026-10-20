module "vpc" {
  source = "../../modules/vpc"

  vpc_cidr          = "10.0.0.0/16"
  availability_zones = ["us-east-1a", "us-east-1b"]
  environment       = "dev"
}

module "rds" {
  source = "../../modules/rds"

  environment            = "dev"
  instance_class         = "db.t3.micro"
  database_subnet_ids    = module.vpc.database_subnet_ids
  rds_security_group_id  = module.vpc.rds_security_group_id
  db_password            = var.db_password
}

module "elasticache" {
  source = "../../modules/elasticache"

  environment                  = "dev"
  node_type                    = "cache.t3.micro"
  database_subnet_ids          = module.vpc.database_subnet_ids
  elasticache_security_group_id = module.vpc.elasticache_security_group_id
  redis_password               = var.redis_password
}

module "alb" {
  source = "../../modules/alb"

  environment            = "dev"
  vpc_id                 = module.vpc.vpc_id
  public_subnet_ids      = module.vpc.public_subnet_ids
  alb_security_group_id  = module.vpc.alb_security_group_id
  logs_bucket_id         = aws_s3_bucket.logs.id
  certificate_arn        = ""
  domain_name            = ""
}

module "ecs" {
  source = "../../modules/ecs"

  environment              = "dev"
  cpu                      = 256
  memory                   = 512
  desired_count            = 1
  app_image                = var.app_image
  db_endpoint              = module.rds.db_instance_endpoint
  db_password              = var.db_password
  redis_endpoint           = module.elasticache.redis_endpoint
  kafka_bootstrap_servers  = var.kafka_bootstrap_servers
  jwt_secret               = var.jwt_secret
  private_subnet_ids       = module.vpc.private_subnet_ids
  ecs_security_group_id    = module.vpc.ecs_security_group_id
  target_group_arn         = module.alb.target_group_arn
  aws_region               = var.aws_region
  account_id               = data.aws_caller_identity.current.account_id
}

resource "aws_s3_bucket" "logs" {
  bucket = "sentinelx-dev-logs-${data.aws_caller_identity.current.account_id}"

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        sse_algorithm = "AES256"
      }
    }
  }

  lifecycle {
    prevent_destroy = false
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "logs" {
  bucket = aws_s3_bucket.logs.id

  rule {
    id     = "expire-old-logs"
    status = "Enabled"
    expiration {
      days = 90
    }
  }
}

data "aws_caller_identity" "current" {}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "db_endpoint" {
  value     = module.rds.db_instance_endpoint
  sensitive = true
}

output "redis_endpoint" {
  value = module.elasticache.redis_endpoint
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "app_image" {
  type      = string
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "redis_password" {
  type      = string
  sensitive = true
}

variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "kafka_bootstrap_servers" {
  type      = string
  default   = ""
}