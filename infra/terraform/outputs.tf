# =====================================================================
#  Outputs
# =====================================================================

output "application_url" {
  description = "Public URL of the portal."
  value       = "http://${aws_lb.main.dns_name}"
}

output "alb_dns_name" {
  description = "Load balancer DNS name; point a CNAME at this."
  value       = aws_lb.main.dns_name
}

output "database_endpoint" {
  description = "RDS endpoint. Reachable only from inside the VPC."
  value       = aws_db_instance.main.endpoint
  sensitive   = true
}

output "attachments_bucket" {
  description = "S3 bucket holding feedback attachments."
  value       = aws_s3_bucket.attachments.id
}

output "backend_ecr_repository" {
  description = "Push backend images here."
  value       = aws_ecr_repository.backend.repository_url
}

output "analytics_ecr_repository" {
  description = "Push analytics images here."
  value       = aws_ecr_repository.analytics.repository_url
}

output "secret_arn" {
  description = "Secrets Manager entry holding the database and JWT credentials."
  value       = aws_secretsmanager_secret.app.arn
}

output "ecs_cluster_name" {
  description = "ECS cluster name, for `aws ecs` CLI commands."
  value       = aws_ecs_cluster.main.name
}
