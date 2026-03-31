variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "cluster_id" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "workload_sg_id" {
  type = string
}

variable "http_listener_arn" {
  type = string
}

variable "https_listener_arn" {
  type    = string
  default = null
}

variable "app_image" {
  type = string
}

variable "app_port" {
  type = number
}

variable "desired_count" {
  type = number
}

variable "cpu" {
  type    = number
  default = 1024
}

variable "memory" {
  type    = number
  default = 2048
}

variable "db_secret_arn" {
  type = string
}

variable "db_host" {
  type = string
}

variable "db_port" {
  type = number
}

variable "db_name" {
  type = string
}

variable "redis_host" {
  type = string
}

variable "redis_port" {
  type = number
}

variable "spring_profile" {
  type = string
}

variable "deployment_environment_name" {
  type = string
}

variable "health_check_path" {
  type    = string
  default = "/api/actuator/health"
}

variable "common_tags" {
  type    = map(string)
  default = {}
}

variable "otel_collector_config_arn" {
  type = string
}

variable "prometheus_arn" {
  type = string
}