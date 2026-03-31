aws_region = "us-east-1"

project     = "payment-system"
environment = "dev"

app_image = "kauapereira/payment-system:latest"
app_port = 8081
app_desired_count = 2
app_cpu = 2048
app_memory = 4096
spring_profile = "sandbox-local"

grafana_image = "grafana/grafana:latest"
grafana_port = 3000
grafana_desired_count = 1
grafana_cpu = 512
grafana_memory = 1024
grafana_admin_user = "admin"

vpc_cidr = "10.0.0.0/16"

public_subnet_cidrs = [
  "10.0.1.0/24",
  "10.0.2.0/24"
]

private_subnet_cidrs = [
  "10.0.11.0/24",
  "10.0.12.0/24"
]

azs = [
  "us-east-1a",
  "us-east-1b"
]

enable_https    = false
certificate_arn = ""

db_name     = "payment_system"
db_username = "postgres"

postgres_engine_version = "17"
rds_instance_class      = "db.t4g.medium"
rds_allocated_storage   = 20
rds_multi_az            = false
rds_backup_retention_period = 7

redis_node_type           = "cache.t4g.small"
redis_num_cache_clusters  = 1