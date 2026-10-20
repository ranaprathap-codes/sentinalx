resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.environment}-sentinelx-redis-subnet-group"
  subnet_ids = var.database_subnet_ids
}

resource "aws_elasticache_replication_group" "main" {
  replication_group_id       = "${var.environment}-sentinelx-redis"
  engine                     = "redis"
  engine_version             = "7.1"
  node_type                  = var.node_type
  number_cache_clusters      = var.environment == "prod" ? 2 : 1
  port                       = 6379
  parameter_group_name       = aws_elasticache_parameter_group.main.name
  subnet_group_name          = aws_elasticache_subnet_group.main.name
  security_group_ids         = [var.elasticache_security_group_id]
  
  automatic_failover_enabled = var.environment == "prod"
  multi_az_enabled          = var.environment == "prod"
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                = var.redis_password
  
  snapshot_retention_limit = 7
  snapshot_window          = "03:00-04:00"
  maintenance_window       = "sun:04:00-sun:05:00"
  
  tags = {
    Name        = "${var.environment}-sentinelx-redis"
    Environment = var.environment
  }
}

resource "aws_elasticache_parameter_group" "main" {
  name   = "${var.environment}-sentinelx-redis-params"
  family = "redis7"

  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }

  parameter {
    name  = "timeout"
    value = "300"
  }

  tags = {
    Name = "${var.environment}-sentinelx-redis-params"
  }
}

output "redis_endpoint" {
  value = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "redis_port" {
  value = aws_elasticache_replication_group.main.port
}

output "redis_reader_endpoint" {
  value = aws_elasticache_replication_group.main.reader_endpoint_address
}

variable "environment" {
  type = string
}

variable "node_type" {
  type = string
}

variable "database_subnet_ids" {
  type = list(string)
}

variable "elasticache_security_group_id" {
  type = string
}

variable "redis_password" {
  type      = string
  sensitive = true
}