package uk.ac.wellcome.display.models

sealed trait WorkAggregationRequest

object WorkAggregationRequest {
  case object Format extends WorkAggregationRequest

  case object ProductionDate extends WorkAggregationRequest

  case object Genre extends WorkAggregationRequest

  case object GenreDeprecated extends WorkAggregationRequest

  case object Subject extends WorkAggregationRequest

  case object SubjectDeprecated extends WorkAggregationRequest

  case object Contributor extends WorkAggregationRequest

  case object ContributorDeprecated extends WorkAggregationRequest

  case object Languages extends WorkAggregationRequest

  case object License extends WorkAggregationRequest

  case object LicenseDeprecated extends WorkAggregationRequest

  case object Availabilities extends WorkAggregationRequest
}
