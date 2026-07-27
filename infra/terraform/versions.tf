# =====================================================================
#  Provider and backend configuration
# =====================================================================

terraform {
  required_version = ">= 1.9.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.70"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Remote state. Uncomment and point at a bucket you own before applying
  # anything real - local state does not survive a laptop.
  #
  # backend "s3" {
  #   bucket         = "sfp-terraform-state"
  #   key            = "student-feedback-portal/terraform.tfstate"
  #   region         = "ap-south-1"
  #   encrypt        = true
  #   dynamodb_table = "sfp-terraform-locks"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "student-feedback-portal"
      Environment = var.environment
      ManagedBy   = "terraform"
      Owner       = "adwitiya"
    }
  }
}
