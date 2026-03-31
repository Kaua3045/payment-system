variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "db_username" {
  type = string
}

variable "db_name" {
  type = string
}

variable "common_tags" {
  type    = map(string)
  default = {}
}