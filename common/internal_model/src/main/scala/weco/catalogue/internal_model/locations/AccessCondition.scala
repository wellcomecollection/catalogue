package weco.catalogue.internal_model.locations

case class AccessCondition(
  method: AccessMethod,
  status: Option[AccessStatus] = None,
  terms: Option[String] = None,
  to: Option[String] = None,
  note: Option[String] = None
) {
  def isEmpty: Boolean =
    this == AccessCondition(method = AccessMethod.NotRequestable)

  def isAvailable: Boolean = status.exists(_.isAvailable)

  def hasRestrictions: Boolean = status.exists(_.hasRestrictions)
}

case object AccessCondition {
  def apply(method: AccessMethod, status: AccessStatus): AccessCondition =
    AccessCondition(method = method, status = Some(status))
}
