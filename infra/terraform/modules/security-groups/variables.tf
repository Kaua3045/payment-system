variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "app_port" {
  type = number
}

variable "grafana_port" {
  type    = number
  default = 3000
}

variable "common_tags" {
  type    = map(string)
  default = {}
}