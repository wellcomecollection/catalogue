data "aws_ssm_parameter" "critical_slack_webhook" {
  name = "/aws/reference/secretsmanager/sierra_adapter/critical_slack_webhook"
}

locals {
  namespace_hyphen = replace(var.namespace, "_", "-")

  sierra_api_url = "https://libsys.wellcomelibrary.org/iii/sierra-api/v5"

  # See https://sandbox.iii.com/iii/sierra-api/swagger/index.html

  sierra_items_fields = join(",", [
    "updatedDate",
    "createdDate",
    "deletedDate",
    "deleted",
    "suppressed",
    "bibIds",
    "location",
    "status",
    "barcode",
    "callNumber",
    "itemType",
    "transitInfo",
    "copyNo",
    "holdCount",
    "fixedFields",
    "varFields"
  ])

  sierra_bibs_fields = join(",", [
    "updatedDate",
    "createdDate",
    "deletedDate",
    "deleted",
    "suppressed",
    "available",
    "lang",
    "title",
    "author",
    "materialType",
    "bibLevel",
    "publishYear",
    "catalogDate",
    "country",
    "orders",
    "normTitle",
    "normAuthor",
    "locations",
    "fixedFields",
    "varFields"
  ])

  sierra_holdings_fields = join(",", [
    "bibIds",
    "itemIds",
    "inheritLocation",
    "allocationRule",
    "accountingUnit",
    "labelCode",
    "serialCode1",
    "serialCode2",
    "claimOnDate",
    "receivingLocationCode",
    "vendorCode",
    "serialCode3",
    "serialCode4",
    "updateCount",
    "pieceCount",
    "eCheckInCode",
    "mediaTypeCode",
    "updatedDate",
    "createdDate",
    "deletedDate",
    "deleted",
    "suppressed",
    "fixedFields",
    "varFields",
  ])

  # See https://techdocs.iii.com/sierraapi/Content/zReference/objects/orderObject.htm
  sierra_orders_fields = join(",", [
    "bibs",
    "updatedDate",
    "createdDate",
    "deletedDate",
    "deleted",
    "suppressed",
    "accountingUnit",
    "estimatedPrice",
    "vendorRecordCode",
    "orderDate",
    "chargedFunds",
    "vendorTitles",
    "fixedFields",
    "varFields",
  ])

  critical_slack_webhook = data.aws_ssm_parameter.critical_slack_webhook.value
}
