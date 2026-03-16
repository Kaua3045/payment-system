resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.project}-${var.environment}-redis-subnets"
  subnet_ids = var.private_subnet_ids
}

resource "aws_elasticache_replication_group" "this" {
  replication_group_id       = "${var.project}-${var.environment}-redis"
  description                = "Redis ${var.project}-${var.environment}"
  engine                     = "redis"
  node_type                  = var.node_type
  num_cache_clusters         = var.num_cache_clusters
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [var.redis_sg_id]
  automatic_failover_enabled = var.num_cache_clusters > 1
  at_rest_encryption_enabled = true
  transit_encryption_enabled = false
}