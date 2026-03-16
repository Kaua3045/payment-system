output "db_password" {
  value     = random_password.db_password.result
  sensitive = true
}

output "db_secret_arn" {
  value = aws_secretsmanager_secret.db_credentials.arn
}

output "db_secret_name" {
  value = aws_secretsmanager_secret.db_credentials.name
}

output "db_username" {
  value = var.db_username
}

output "db_name" {
  value = var.db_name
}