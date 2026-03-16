resource "aws_ecs_cluster" "this" {
  name = "${var.project}-${var.environment}"

  tags = merge(var.common_tags, {
    Name = "${var.project}-${var.environment}-ecs-cluster"
  })
}