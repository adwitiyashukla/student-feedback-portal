variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "ap-south-1"
}

variable "environment" {
  description = "Environment name, used in resource names and tags."
  type        = string
  default     = "prod"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod."
  }
}

variable "project_name" {
  description = "Short slug prefixed to every resource name."
  type        = string
  default     = "sfp"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "availability_zone_count" {
  description = "How many AZs to spread subnets across. Two is the ALB minimum."
  type        = number
  default     = 2

  validation {
    condition     = var.availability_zone_count >= 2
    error_message = "An Application Load Balancer requires at least two availability zones."
  }
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "Initial storage in GiB."
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Initial database name."
  type        = string
  default     = "feedback_portal"
}

variable "db_username" {
  description = "Master username. The password is generated and stored in Secrets Manager."
  type        = string
  default     = "feedback_admin"
}

variable "db_multi_az" {
  description = "Run a standby in a second AZ. Roughly doubles cost."
  type        = bool
  default     = false
}

variable "db_backup_retention_days" {
  description = "Automated backup retention window."
  type        = number
  default     = 7
}

variable "backend_image" {
  description = "Backend image URI. Defaults to the ECR repository created here."
  type        = string
  default     = ""
}

variable "analytics_image" {
  description = "Analytics image URI. Defaults to the ECR repository created here."
  type        = string
  default     = ""
}

variable "backend_cpu" {
  description = "Fargate CPU units for the backend task."
  type        = number
  default     = 1024
}

variable "backend_memory" {
  description = "Fargate memory (MiB) for the backend task."
  type        = number
  default     = 2048
}

variable "backend_desired_count" {
  description = "Number of backend tasks to run."
  type        = number
  default     = 2
}

variable "analytics_cpu" {
  description = "Fargate CPU units for the analytics task."
  type        = number
  default     = 512
}

variable "analytics_memory" {
  description = "Fargate memory (MiB) for the analytics task."
  type        = number
  default     = 1024
}

variable "analytics_desired_count" {
  description = "Number of analytics tasks to run."
  type        = number
  default     = 1
}

variable "app_base_url" {
  description = "Public URL, used to build links in notification email."
  type        = string
  default     = "http://localhost"
}

variable "bootstrap_admin_email" {
  description = "Email for the super-administrator created on an empty database."
  type        = string
  default     = "admin@university.edu"
}

variable "log_retention_days" {
  description = "CloudWatch log retention."
  type        = number
  default     = 30
}
