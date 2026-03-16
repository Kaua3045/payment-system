variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "redis_sg_id" {
  type = string
}

variable "node_type" {
  type = string
}

variable "num_cache_clusters" {
  type = number
}

variable "common_tags" {
  type    = map(string)
  default = {}
}