output "alb_sg_id" {
  value = aws_security_group.alb.id
}

output "workload_sg_id" {
  value = aws_security_group.workload.id
}

output "rds_sg_id" {
  value = aws_security_group.rds.id
}

output "redis_sg_id" {
  value = aws_security_group.redis.id
}