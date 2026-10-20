resource "aws_db_subnet_group" "main" {
  name       = "${var.environment}-sentinelx-db-subnet-group"
  subnet_ids = var.database_subnet_ids

  tags = {
    Name = "${var.environment}-sentinelx-db-subnet-group"
  }
}

resource "aws_db_instance" "main" {
  identifier             = "${var.environment}-sentinelx-db"
  engine                 = "postgres"
  engine_version         = "16.3"
  instance_class         = var.instance_class
  allocated_storage      = 20
  max_allocated_storage  = 100
  storage_encrypted      = true
  storage_type           = "gp3"
  
  db_name  = "sentinelx"
  username = "sentinelx"
  password = var.db_password
  
  vpc_security_group_ids = [var.rds_security_group_id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"
  
  skip_final_snapshot = var.environment == "dev"
  deletion_protection   = var.environment != "dev"
  
  performance_insights_enabled = true
  monitoring_interval         = 60
  monitoring_role_arn         = aws_iam_role.rds_monitoring.arn
  
  tags = {
    Name        = "${var.environment}-sentinelx-db"
    Environment = var.environment
  }
}

resource "aws_iam_role" "rds_monitoring" {
  name = "${var.environment}-sentinelx-rds-monitoring"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "monitoring.rds.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

resource "aws_db_parameter_group" "main" {
  name   = "${var.environment}-sentinelx-db-params"
  family = "postgres16"

  parameter {
    name  = "shared_preload_libraries"
    value = "pg_stat_statements"
  }

  parameter {
    name  = "pg_stat_statements.track"
    value = "all"
  }

  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }

  tags = {
    Name = "${var.environment}-sentinelx-db-params"
  }
}

output "db_instance_endpoint" {
  value = aws_db_instance.main.endpoint
}

output "db_instance_port" {
  value = aws_db_instance.main.port
}

output "db_instance_arn" {
  value = aws_db_instance.main.arn
}

variable "environment" {
  type = string
}

variable "instance_class" {
  type = string
}

variable "database_subnet_ids" {
  type = list(string)
}

variable "rds_security_group_id" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
}