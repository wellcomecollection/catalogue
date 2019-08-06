# RDS

output "rds_access_security_group_id" {
  value = "${aws_security_group.rds_ingress_security_group.id}"
}

# Miro Hybrid Store

output "vhs_miro_read_policy" {
  value = "${module.vhs_miro.read_policy}"
}

output "vhs_miro_table_name" {
  value = "${module.vhs_miro.table_name}"
}

output "vhs_miro_assumable_read_role" {
  value = "${module.vhs_miro.assumable_read_role}"
}

# Miro Inventory Hybrid Store

output "vhs_miro_inventory_table_name" {
  value = "${module.vhs_miro_migration.table_name}"
}

output "vhs_miro_inventory_assumable_read_role" {
  value = "${module.vhs_miro_migration.assumable_read_role}"
}

# Sierra Hybrid Store

output "vhs_sierra_full_access_policy" {
  value = "${module.vhs_sierra.full_access_policy}"
}

output "vhs_sierra_read_policy" {
  value = "${module.vhs_sierra.read_policy}"
}

output "vhs_sierra_table_name" {
  value = "${module.vhs_sierra.table_name}"
}

output "vhs_sierra_bucket_name" {
  value = "${module.vhs_sierra.bucket_name}"
}

output "vhs_sierra_assumable_read_role" {
  value = "${module.vhs_sierra.assumable_read_role}"
}

# Sierra Items Hybrid Store

output "vhs_sierra_items_full_access_policy" {
  value = "${module.vhs_sierra_items.full_access_policy}"
}

output "vhs_sierra_items_table_name" {
  value = "${module.vhs_sierra_items.table_name}"
}

output "vhs_sierra_items_bucket_name" {
  value = "${module.vhs_sierra_items.bucket_name}"
}

output "vhs_sierra_items_assumable_read_role" {
  value = "${module.vhs_sierra_items.assumable_read_role}"
}

# Goobi Hybrid Store

output "vhs_goobi_full_access_policy" {
  value = "${module.vhs_goobi_mets.full_access_policy}"
}

output "vhs_goobi_table_name" {
  value = "${module.vhs_goobi_mets.table_name}"
}

output "vhs_goobi_bucket_name" {
  value = "${module.vhs_goobi_mets.bucket_name}"
}

output "vhs_goobi_assumable_read_role" {
  value = "${module.vhs_goobi_mets.assumable_read_role}"
}

# Calm Hybrid Store
output "vhs_calm_read_policy" {
  value = "${module.vhs_calm_sourcedata.read_policy}"
}

output "vhs_calm_table_name" {
  value = "${module.vhs_calm_sourcedata.table_name}"
}

output "vhs_calm_bucket_name" {
  value = "${module.vhs_calm_sourcedata.bucket_name}"
}

output "vhs_calm_assumable_read_role" {
  value = "${module.vhs_calm_sourcedata.assumable_read_role}"
}
