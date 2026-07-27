# =====================================================================
#  IAM roles for the ECS tasks
#
#  Two roles per task, on purpose. The execution role is what the ECS
#  agent uses to pull an image and read secrets before the container
#  starts. The task role is what the application itself gets. Keeping
#  them separate means the running process cannot read the secrets it
#  was launched with.
# =====================================================================

data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "task_execution" {
  name               = "${local.name}-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "task_execution_managed" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "read_secrets" {
  statement {
    effect    = "Allow"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = [aws_secretsmanager_secret.app.arn]
  }
}

resource "aws_iam_role_policy" "task_execution_secrets" {
  name   = "${local.name}-read-secrets"
  role   = aws_iam_role.task_execution.id
  policy = data.aws_iam_policy_document.read_secrets.json
}

# ---------------------------------------------------------------------
# Task role - the backend's own permissions at runtime
# ---------------------------------------------------------------------

resource "aws_iam_role" "backend_task" {
  name               = "${local.name}-backend-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

data "aws_iam_policy_document" "attachments_access" {
  # Object-level operations on the attachment bucket, and nothing else.
  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]
    resources = ["${aws_s3_bucket.attachments.arn}/*"]
  }

  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.attachments.arn]
  }
}

resource "aws_iam_role_policy" "backend_attachments" {
  name   = "${local.name}-attachments"
  role   = aws_iam_role.backend_task.id
  policy = data.aws_iam_policy_document.attachments_access.json
}

resource "aws_iam_role" "analytics_task" {
  name               = "${local.name}-analytics-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}
