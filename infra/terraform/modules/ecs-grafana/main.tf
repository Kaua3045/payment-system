data "aws_iam_policy_document" "ecs_task_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "grafana_amp_query" {
  name = "${var.project}-${var.environment}-grafana-amp-query"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "aps:QueryMetrics",
          "aps:GetSeries",
          "aps:GetLabels",
          "aps:GetMetricMetadata"
        ]
        Resource = var.prometheus_arn
      }
    ]
  })
}

resource "aws_iam_role" "execution_role" {
  name               = "${var.project}-${var.environment}-grafana-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = var.common_tags
}

resource "aws_iam_role_policy_attachment" "execution_default" {
  role       = aws_iam_role.execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task_role" {
  name               = "${var.project}-${var.environment}-grafana-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = var.common_tags
}

resource "aws_iam_role_policy_attachment" "grafana_amp_query" {
  role       = aws_iam_role.task_role.name
  policy_arn = aws_iam_policy.grafana_amp_query.arn
}

resource "aws_cloudwatch_log_group" "grafana" {
  name              = "/ecs/${var.project}-${var.environment}/grafana"
  retention_in_days = 7

  tags = var.common_tags
}

resource "aws_lb_target_group" "grafana" {
  name        = substr("${var.project}-${var.environment}-grafana-tg", 0, 32)
  port        = var.grafana_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.vpc_id

  health_check {
    path                = "/api/health"
    matcher             = "200"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
  }

  tags = var.common_tags
}

resource "aws_lb_listener_rule" "grafana_http" {
  listener_arn = var.http_listener_arn
  priority     = 100

  condition {
    path_pattern {
      values = ["/grafana", "/grafana/*"]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grafana.arn
  }
}

resource "aws_lb_listener_rule" "grafana_https" {
  count        = var.https_listener_arn != null ? 1 : 0
  listener_arn = var.https_listener_arn
  priority     = 100

  condition {
    path_pattern {
      values = ["/grafana", "/grafana/*"]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grafana.arn
  }
}

resource "aws_ecs_task_definition" "grafana" {
  family                   = "${var.project}-${var.environment}-grafana"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = tostring(var.cpu)
  memory                   = tostring(var.memory)
  execution_role_arn       = aws_iam_role.execution_role.arn
  task_role_arn            = aws_iam_role.task_role.arn

  container_definitions = jsonencode([
    {
      name      = "grafana"
      image     = var.grafana_image
      essential = true

      portMappings = [
        {
          containerPort = var.grafana_port
          hostPort      = var.grafana_port
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "GF_SECURITY_ADMIN_USER", value = var.grafana_admin_user },
        { name = "GF_SECURITY_ADMIN_PASSWORD", value = var.grafana_admin_password },
        { name = "GF_AUTH_ANONYMOUS_ENABLED", value = "false" },
        { name = "GF_USERS_ALLOW_SIGN_UP", value = "false" },

        { name = "GF_SERVER_PROTOCOL", value = "http" },
        { name = "GF_SERVER_HTTP_PORT", value = tostring(var.grafana_port) },
        { name = "GF_SERVER_ROOT_URL", value = "%(protocol)s://%(domain)s/grafana/" },
        { name = "GF_SERVER_SERVE_FROM_SUB_PATH", value = "true" },

        { name = "AWS_SDK_LOAD_CONFIG", value = "true" },
        { name = "GF_AUTH_SIGV4_AUTH_ENABLED", value = "true"}
      ]

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:${var.grafana_port}/api/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.grafana.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])

  tags = var.common_tags
}

resource "aws_ecs_service" "grafana" {
  name                              = "${var.project}-${var.environment}-grafana"
  cluster                           = var.cluster_id
  task_definition                   = aws_ecs_task_definition.grafana.arn
  desired_count                     = var.desired_count
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 90

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.workload_sg_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.grafana.arn
    container_name   = "grafana"
    container_port   = var.grafana_port
  }

  depends_on = [
    aws_lb_listener_rule.grafana_http
  ]

  tags = var.common_tags
}