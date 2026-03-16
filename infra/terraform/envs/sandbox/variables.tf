variable "aws_region" {
  type = string
}

variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "public_subnet_cidrs" {
  type = list(string)
}

variable "private_subnet_cidrs" {
  type = list(string)
}

variable "azs" {
  type = list(string)
}

variable "enable_https" {
  type    = bool
  default = false
}

variable "certificate_arn" {
  type    = string
  default = null
}

variable "db_name" {
  type = string
}

variable "db_username" {
  type = string
}

variable "postgres_engine_version" {
  type = string
}

variable "rds_instance_class" {
  type = string
}

variable "rds_allocated_storage" {
  type = number
}

variable "rds_multi_az" {
  type    = bool
  default = false
}

variable "rds_backup_retention_period" {
  type    = number
  default = 7
}

variable "redis_node_type" {
  type = string
}

variable "redis_num_cache_clusters" {
  type = number
}

variable "app_image" {
  type = string
}

variable "app_port" {
  type = number
}

variable "app_desired_count" {
  type = number
}

variable "app_cpu" {
  type    = number
  default = 1024
}

variable "app_memory" {
  type    = number
  default = 2048
}

variable "spring_profile" {
  type = string
}

variable "grafana_image" {
  type = string
}

variable "grafana_port" {
  type    = number
  default = 3000
}

variable "grafana_desired_count" {
  type    = number
  default = 1
}

variable "grafana_cpu" {
  type    = number
  default = 512
}

variable "grafana_memory" {
  type    = number
  default = 1024
}

variable "grafana_admin_user" {
  type = string
}

variable "grafana_admin_password" {
  type      = string
  sensitive = true
}