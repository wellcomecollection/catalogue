output "task_role_name" {
  value = module.worker.task_role_name
}

output "task_role_arn" {
  value = module.worker.task_role_arn
}

output "scale_up_arn" {
  value = module.worker.scale_up_arn
}

output "scale_down_arn" {
  value = module.worker.scale_down_arn
}
