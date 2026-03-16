resource "aws_db_subnet_group" "this" {
  name       = "${var.project}-${var.environment}-db-subnets"
  subnet_ids = var.private_subnet_ids

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-db-subnets"
  })
}

resource "aws_db_instance" "this" {
  identifier                 = "${var.project}-${var.environment}-postgres"
  engine                     = "postgres"
  engine_version             = var.postgres_engine_version
  instance_class             = var.instance_class
  allocated_storage          = var.allocated_storage
  db_name                    = var.db_name
  username                   = var.db_username
  password                   = var.db_password
  port                       = 5432
  db_subnet_group_name       = aws_db_subnet_group.this.name
  vpc_security_group_ids     = [var.rds_sg_id]
  publicly_accessible        = false
  multi_az                   = var.multi_az
  storage_encrypted          = true
  backup_retention_period    = var.backup_retention_period
  skip_final_snapshot        = var.skip_final_snapshot
  deletion_protection        = var.deletion_protection
  auto_minor_version_upgrade = true

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-postgres"
  })
}