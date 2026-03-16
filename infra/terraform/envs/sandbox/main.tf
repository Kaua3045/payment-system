locals {
  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
    Stack       = "base"
  }
}

module "network" {
  source = "../../modules/network"

  project              = var.project
  environment          = var.environment
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  azs                  = var.azs
  common_tags          = local.common_tags
}

module "security_groups" {
  source = "../../modules/security-groups"

  project      = var.project
  environment  = var.environment
  vpc_id       = module.network.vpc_id
  app_port     = var.app_port
  grafana_port = var.grafana_port
  common_tags  = local.common_tags
}

module "secrets" {
  source = "../../modules/secrets"

  project     = var.project
  environment = var.environment
  db_username = var.db_username
  db_name     = var.db_name
  common_tags = local.common_tags
}

module "rds" {
  source = "../../modules/rds"

  project                 = var.project
  environment             = var.environment
  private_subnet_ids      = module.network.private_subnet_ids
  rds_sg_id               = module.security_groups.rds_sg_id
  db_name                 = var.db_name
  db_username             = var.db_username
  db_password             = module.secrets.db_password
  postgres_engine_version = var.postgres_engine_version
  instance_class          = var.rds_instance_class
  allocated_storage       = var.rds_allocated_storage
  multi_az                = var.rds_multi_az
  backup_retention_period = var.rds_backup_retention_period
  common_tags             = local.common_tags
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = module.secrets.db_secret_arn

  secret_string = jsonencode({
    username = var.db_username
    password = module.secrets.db_password
    engine   = "postgres"
    host     = module.rds.address
    port     = module.rds.port
    dbname   = var.db_name
  })
}

module "redis" {
  source = "../../modules/redis"

  project            = var.project
  environment        = var.environment
  private_subnet_ids = module.network.private_subnet_ids
  redis_sg_id        = module.security_groups.redis_sg_id
  node_type          = var.redis_node_type
  num_cache_clusters = var.redis_num_cache_clusters
  common_tags        = local.common_tags
}

module "alb" {
  source = "../../modules/alb"

  project           = var.project
  environment       = var.environment
  vpc_id            = module.network.vpc_id
  public_subnet_ids = module.network.public_subnet_ids
  alb_sg_id         = module.security_groups.alb_sg_id
  enable_https      = var.enable_https
  certificate_arn   = var.certificate_arn
  common_tags       = local.common_tags
}

module "ecs_cluster" {
  source = "../../modules/ecs-cluster"

  project     = var.project
  environment = var.environment
  common_tags = local.common_tags
}

module "ecs_grafana" {
  source = "../../modules/ecs-grafana"

  project                = var.project
  environment            = var.environment
  aws_region             = var.aws_region
  cluster_id             = module.ecs_cluster.id
  vpc_id                 = module.network.vpc_id
  private_subnet_ids     = module.network.private_subnet_ids
  workload_sg_id         = module.security_groups.workload_sg_id
  http_listener_arn      = module.alb.http_listener_arn
  https_listener_arn     = module.alb.https_listener_arn
  grafana_image          = var.grafana_image
  grafana_port           = var.grafana_port
  desired_count          = var.grafana_desired_count
  cpu                    = var.grafana_cpu
  memory                 = var.grafana_memory
  grafana_admin_user     = var.grafana_admin_user
  grafana_admin_password = var.grafana_admin_password // TODO change this
  prometheus_arn         = aws_prometheus_workspace.this.arn
  common_tags            = local.common_tags
}

module "ecs_app" {
  source = "../../modules/ecs-app"

  project                     = var.project
  environment                 = var.environment
  aws_region                  = var.aws_region
  cluster_id                  = module.ecs_cluster.id
  cluster_name                = module.ecs_cluster.name
  vpc_id                      = module.network.vpc_id
  private_subnet_ids          = module.network.private_subnet_ids
  workload_sg_id              = module.security_groups.workload_sg_id
  http_listener_arn           = module.alb.http_listener_arn
  https_listener_arn          = module.alb.https_listener_arn
  app_image                   = var.app_image
  app_port                    = var.app_port
  desired_count               = var.app_desired_count
  cpu                         = var.app_cpu
  memory                      = var.app_memory
  db_secret_arn               = module.secrets.db_secret_arn
  db_host                     = module.rds.address
  db_port                     = module.rds.port
  db_name                     = var.db_name
  redis_host                  = module.redis.primary_endpoint_address
  redis_port                  = module.redis.port
  spring_profile              = var.spring_profile
  deployment_environment_name = var.environment
  otel_collector_config_arn   = aws_ssm_parameter.otel_collector_config.arn
  prometheus_arn              = aws_prometheus_workspace.this.arn
  common_tags                 = local.common_tags

  depends_on = [module.ecs_grafana]
}

resource "aws_prometheus_workspace" "this" {
  alias = "${var.project}-${var.environment}"
}

resource "aws_ssm_parameter" "otel_collector_config" {
  name = "/${var.project}/${var.environment}/otel-collector-config"
  type = "String"

  value = templatefile("${path.module}/collector-config.yaml.tftpl", {
    aws_region                = var.aws_region
    amp_remote_write_endpoint = "${aws_prometheus_workspace.this.prometheus_endpoint}api/v1/remote_write"
  })
}

resource "null_resource" "grafana_bootstrap" {
  depends_on = [
    module.ecs_grafana,
    aws_prometheus_workspace.this
  ]

  triggers = {
    grafana_url        = module.alb.alb_dns_name
    prometheus_url     = aws_prometheus_workspace.this.prometheus_endpoint
    dashboard_checksum = filesha256("${path.module}/payment-system-dashboard.json")
    grafana_user       = var.grafana_admin_user
    grafana_password   = var.grafana_admin_password // TODO change this
    https_enabled      = tostring(var.enable_https)
  }

  provisioner "local-exec" {
    interpreter = ["/bin/bash", "-lc"]
    command     = <<-EOT
      set -euo pipefail

      if [ "${var.enable_https}" = "true" ]; then
        GRAFANA_URL="https://${module.alb.alb_dns_name}/grafana"
      else
        GRAFANA_URL="http://${module.alb.alb_dns_name}/grafana"
      fi

      USER="${var.grafana_admin_user}"
      PASS="${var.grafana_admin_password}"
      AMP_URL="${aws_prometheus_workspace.this.prometheus_endpoint}"

      echo "Waiting for Grafana..."
      READY=0
      for i in $(seq 1 60); do
        if curl -k -sf -u "$${USER}:$${PASS}" "$${GRAFANA_URL}/api/health" >/dev/null; then
          READY=1
          break
        fi
        sleep 10
      done

      if [ "$${READY}" -ne 1 ]; then
        echo "Grafana did not become ready"
        exit 1
      fi

      echo "Creating datasource..."
      curl -k -sf -X POST "$${GRAFANA_URL}/api/datasources" \
        -u "$${USER}:$${PASS}" \
        -H "Content-Type: application/json" \
        -d @- <<JSON || true
{
  "name": "amp-prometheus",
  "uid": "prometheus",
  "type": "prometheus",
  "url": "$${AMP_URL}",
  "access": "proxy",
  "isDefault": true,
  "jsonData": {
    "httpMethod": "POST",
    "sigV4Auth": true,
    "sigV4AuthType": "default",
    "sigV4Region": "${var.aws_region}"
  }
}
JSON

      echo "Preparing dashboard payload..."
      python3 - <<'PY'
import json
from pathlib import Path

dash_path = Path("${path.module}/payment-system-dashboard.json")
payload_path = Path("${path.module}/dashboard-payload.json")

dashboard = json.loads(dash_path.read_text())
dashboard.pop("id", None)

payload = {
    "dashboard": dashboard,
    "overwrite": True
}

payload_path.write_text(json.dumps(payload))
print(f"Payload written to {payload_path}")
PY

      echo "Uploading dashboard..."
      HTTP_CODE=$(curl -k -sS -o "${path.module}/dashboard-response.json" -w "%%{http_code}" \
        -X POST "$${GRAFANA_URL}/api/dashboards/db" \
        -u "$${USER}:$${PASS}" \
        -H "Content-Type: application/json" \
        --data-binary @"${path.module}/dashboard-payload.json")

      echo "Grafana dashboard upload HTTP status: $${HTTP_CODE}"
      cat "${path.module}/dashboard-response.json"

      if [ "$${HTTP_CODE}" -lt 200 ] || [ "$${HTTP_CODE}" -ge 300 ]; then
        exit 1
      fi
    EOT
  }
}
