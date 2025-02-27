package uk.gov.justice.digital.hmpps.scheduled.model

data class EventBridgeEvent(
  val payload: Map<String, Any>,
) {
  val detail: Map<String, Any> by payload
  //val detail: String by payload
  val source: String by payload
  val detailType: String = payload.getOrDefault("detail-type", "") as String
}

data class DatasetGenerateEvent(
  val datasetId: String,
  val productDefinitionId : String,
  val category: String
)