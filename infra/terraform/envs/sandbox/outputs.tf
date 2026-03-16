output "vpc_id" {
  value = module.network.vpc_id
}

output "public_subnet_ids" {
  value = module.network.public_subnet_ids
}

output "private_subnet_ids" {
  value = module.network.private_subnet_ids
}

output "alb_sg_id" {
  value = module.security_groups.alb_sg_id
}

output "workload_sg_id" {
  value = module.security_groups.workload_sg_id
}

output "rds_sg_id" {
  value = module.security_groups.rds_sg_id
}

output "redis_sg_id" {
  value = module.security_groups.redis_sg_id
}

output "rds_address" {
  value = module.rds.address
}

output "rds_port" {
  value = module.rds.port
}

output "db_secret_arn" {
  value = module.secrets.db_secret_arn
}

output "db_secret_name" {
  value = module.secrets.db_secret_name
}

output "redis_primary_endpoint_address" {
  value = module.redis.primary_endpoint_address
}

output "redis_port" {
  value = module.redis.port
}

output "alb_arn" {
  value = module.alb.alb_arn
}

output "alb_dns_name" {
  value = module.alb.alb_dns_name
}

output "http_listener_arn" {
  value = module.alb.http_listener_arn
}

output "ecs_cluster_name" {
  value = module.ecs_cluster.name
}

output "app_service_name" {
  value = module.ecs_app.service_name
}

output "grafana_service_name" {
  value = module.ecs_grafana.service_name
}