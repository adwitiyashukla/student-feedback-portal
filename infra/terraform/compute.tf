# =====================================================================
#  ALB, ECS cluster, task definitions and services
# =====================================================================

locals {
  backend_image   = var.backend_image != "" ? var.backend_image : "${aws_ecr_repository.backend.repository_url}:latest"
  analytics_image = var.analytics_image != "" ? var.analytics_image : "${aws_ecr_repository.analytics.repository_url}:latest"
}

# ---------------------------------------------------------------------
# Load balancer
# ---------------------------------------------------------------------

resource "aws_lb" "main" {
  name               = "${local.name}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  enable_deletion_protection = var.environment == "prod"
  drop_invalid_header_fields = true
  idle_timeout               = 60

  tags = { Name = "${local.name}-alb" }
}

resource "aws_lb_target_group" "backend" {
  name        = "${local.name}-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/api/v1/health"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # Give in-flight requests time to finish during a rolling deploy.
  deregistration_delay = 30

  stickiness {
    type            = "lb_cookie"
    enabled         = true
    cookie_duration = 86400
  }
}

# Port 80 exists only to redirect. Nothing is served over plain HTTP.
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# Requires an ACM certificate. Left commented so `terraform plan` works
# before a domain exists; uncomment once var.certificate_arn is supplied.
#
# resource "aws_lb_listener" "https" {
#   load_balancer_arn = aws_lb.main.arn
#   port              = 443
#   protocol          = "HTTPS"
#   ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
#   certificate_arn   = var.certificate_arn
#
#   default_action {
#     type             = "forward"
#     target_group_arn = aws_lb_target_group.backend.arn
#   }
# }

# ---------------------------------------------------------------------
# Logging
# ---------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/${local.name}/backend"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_log_group" "analytics" {
  name              = "/ecs/${local.name}/analytics"
  retention_in_days = var.log_retention_days
}

# ---------------------------------------------------------------------
# Cluster
# ---------------------------------------------------------------------

resource "aws_ecs_cluster" "main" {
  name = "${local.name}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_service_discovery_private_dns_namespace" "internal" {
  name        = "${local.name}.internal"
  description = "Service discovery for backend-to-analytics traffic"
  vpc         = aws_vpc.main.id
}

resource "aws_service_discovery_service" "analytics" {
  name = "analytics"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.internal.id

    dns_records {
      type = "A"
      ttl  = 10
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

# ---------------------------------------------------------------------
# Analytics task and service
# ---------------------------------------------------------------------

resource "aws_ecs_task_definition" "analytics" {
  family                   = "${local.name}-analytics"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.analytics_cpu
  memory                   = var.analytics_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.analytics_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name      = "analytics"
    image     = local.analytics_image
    essential = true

    portMappings = [{ containerPort = 8000, protocol = "tcp" }]

    environment = [
      { name = "ANALYTICS_MODEL_DIR", value = "/app/data" },
      { name = "ANALYTICS_DEBUG", value = "false" },
    ]

    secrets = [
      { name = "ANALYTICS_API_KEY", valueFrom = "${aws_secretsmanager_secret.app.arn}:ANALYTICS_API_KEY::" },
    ]

    healthCheck = {
      command     = ["CMD-SHELL", "curl -fsS http://localhost:8000/api/v1/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 30
    }

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.analytics.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "analytics"
      }
    }
  }])
}

resource "aws_ecs_service" "analytics" {
  name            = "${local.name}-analytics"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.analytics.arn
  desired_count   = var.analytics_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.analytics.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.analytics.arn
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }
}

# ---------------------------------------------------------------------
# Backend task and service
# ---------------------------------------------------------------------

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.backend_task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([{
    name      = "backend"
    image     = local.backend_image
    essential = true

    portMappings = [{ containerPort = 8080, protocol = "tcp" }]

    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "APP_BASE_URL", value = var.app_base_url },
      { name = "STORAGE_TYPE", value = "S3" },
      { name = "AWS_S3_BUCKET", value = aws_s3_bucket.attachments.id },
      { name = "AWS_REGION", value = var.aws_region },
      { name = "ANALYTICS_BASE_URL", value = "http://analytics.${aws_service_discovery_private_dns_namespace.internal.name}:8000" },
      { name = "BOOTSTRAP_ADMIN_EMAIL", value = var.bootstrap_admin_email },
      { name = "COOKIE_SECURE", value = "true" },
    ]

    secrets = [
      { name = "DB_URL", valueFrom = "${aws_secretsmanager_secret.app.arn}:DB_URL::" },
      { name = "DB_USERNAME", valueFrom = "${aws_secretsmanager_secret.app.arn}:DB_USERNAME::" },
      { name = "DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.app.arn}:DB_PASSWORD::" },
      { name = "JWT_SECRET", valueFrom = "${aws_secretsmanager_secret.app.arn}:JWT_SECRET::" },
      { name = "ANALYTICS_API_KEY", valueFrom = "${aws_secretsmanager_secret.app.arn}:ANALYTICS_API_KEY::" },
    ]

    healthCheck = {
      command     = ["CMD-SHELL", "curl -fsS http://localhost:8080/api/v1/health || exit 1"]
      interval    = 30
      timeout     = 5
      retries     = 3
      startPeriod = 120
    }

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.backend.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "backend"
      }
    }
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "${local.name}-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend_desired_count
  launch_type     = "FARGATE"

  # Flyway runs on startup, so the first task needs time before the ALB
  # starts failing it.
  health_check_grace_period_seconds = 180

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.backend.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  depends_on = [aws_lb_listener.http, aws_ecs_service.analytics]
}

# ---------------------------------------------------------------------
# Autoscaling on CPU
# ---------------------------------------------------------------------

resource "aws_appautoscaling_target" "backend" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.backend.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = var.backend_desired_count
  max_capacity       = var.backend_desired_count * 4
}

resource "aws_appautoscaling_policy" "backend_cpu" {
  name               = "${local.name}-backend-cpu"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.backend.service_namespace
  resource_id        = aws_appautoscaling_target.backend.resource_id
  scalable_dimension = aws_appautoscaling_target.backend.scalable_dimension

  target_tracking_scaling_policy_configuration {
    target_value       = 65.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
