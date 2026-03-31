resource "aws_security_group" "alb" {
  name        = "${var.project}-${var.environment}-alb-sg"
  description = "ALB public ingress"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-alb-sg"
  })
}

resource "aws_security_group" "workload" {
  name        = "${var.project}-${var.environment}-workload-sg"
  description = "Shared workload SG for ECS workloads"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-workload-sg"
  })
}

resource "aws_security_group" "rds" {
  name        = "${var.project}-${var.environment}-rds-sg"
  description = "Postgres access from workloads"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-rds-sg"
  })
}

resource "aws_security_group" "redis" {
  name        = "${var.project}-${var.environment}-redis-sg"
  description = "Redis access from workloads"
  vpc_id      = var.vpc_id

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-redis-sg"
  })
}

resource "aws_security_group_rule" "alb_ingress_http" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  description       = "HTTP public"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_ingress_https" {
  type              = "ingress"
  security_group_id = aws_security_group.alb.id
  description       = "HTTPS public"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb_egress_app" {
  type                     = "egress"
  security_group_id        = aws_security_group.alb.id
  description              = "App traffic to workload"
  from_port                = var.app_port
  to_port                  = var.app_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.workload.id
}

resource "aws_security_group_rule" "alb_egress_grafana" {
  type                     = "egress"
  security_group_id        = aws_security_group.alb.id
  description              = "Grafana traffic to workload"
  from_port                = var.grafana_port
  to_port                  = var.grafana_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.workload.id
}

resource "aws_security_group_rule" "workload_ingress_app" {
  type                     = "ingress"
  security_group_id        = aws_security_group.workload.id
  description              = "App traffic from ALB"
  from_port                = var.app_port
  to_port                  = var.app_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
}

resource "aws_security_group_rule" "workload_ingress_grafana" {
  type                     = "ingress"
  security_group_id        = aws_security_group.workload.id
  description              = "Grafana traffic from ALB"
  from_port                = var.grafana_port
  to_port                  = var.grafana_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
}

resource "aws_security_group_rule" "workload_egress_rds" {
  type                     = "egress"
  security_group_id        = aws_security_group.workload.id
  description              = "Postgres to RDS"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.rds.id
}

resource "aws_security_group_rule" "workload_egress_redis" {
  type                     = "egress"
  security_group_id        = aws_security_group.workload.id
  description              = "Redis to cache"
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.redis.id
}

resource "aws_security_group_rule" "workload_egress_https" {
  type              = "egress"
  security_group_id = aws_security_group.workload.id
  description       = "HTTPS outbound"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "workload_egress_http" {
  type              = "egress"
  security_group_id = aws_security_group.workload.id
  description       = "HTTP outbound"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "rds_ingress_workload" {
  type                     = "ingress"
  security_group_id        = aws_security_group.rds.id
  description              = "Postgres from workload"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.workload.id
}

resource "aws_security_group_rule" "rds_egress_vpc" {
  type              = "egress"
  security_group_id = aws_security_group.rds.id
  description       = "Restrict outbound to VPC"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = [var.vpc_cidr]
}

resource "aws_security_group_rule" "redis_ingress_workload" {
  type                     = "ingress"
  security_group_id        = aws_security_group.redis.id
  description              = "Redis from workload"
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.workload.id
}

resource "aws_security_group_rule" "redis_egress_vpc" {
  type              = "egress"
  security_group_id = aws_security_group.redis.id
  description       = "Restrict outbound to VPC"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = [var.vpc_cidr]
}

