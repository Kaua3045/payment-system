resource "random_password" "db_password" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "aws_secretsmanager_secret" "db_credentials" {
  name        = "${var.project}/${var.environment}/databasessss"
  description = "Database credentials for ${var.project}-${var.environment}"

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-db-secret"
  })
}