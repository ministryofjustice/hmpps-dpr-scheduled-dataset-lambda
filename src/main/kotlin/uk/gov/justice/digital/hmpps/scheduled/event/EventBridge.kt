package uk.gov.justice.digital.hmpps.scheduled.event

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.google.gson.Gson
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.eventbridge.model.*
import software.amazon.awssdk.services.eventbridge.model.Target
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetGenerateEvent
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport

class EventBridge(
  val eventBridgeClient: EventBridgeClient,
  private val gson: Gson = Gson(),
) {

  companion object {
    const val RULE_SOURCE = "uk.gov.justice.digital.hmpps.scheduled.lambda.ReportSchedulerLambda"
    const val RULE_DETAIL_TYPE_DATASET = "RedshiftDatasetGenerate"
    const val EVENT_BUS_NAME = "dpr-event-bus"
  }

  fun sendDatasetEvent(event: DatasetGenerateEvent, logger: LambdaLogger) {

    val payload = gson.toJson(event)
    logger.log("attempting to send payload $payload")
    val entry = PutEventsRequestEntry.builder()
      .source(RULE_SOURCE)
      .eventBusName(EVENT_BUS_NAME)
      .detail(payload)
      .detailType(RULE_DETAIL_TYPE_DATASET)
      .build()

    val eventsRequest = PutEventsRequest.builder()
      .entries(entry)
      .build()

    logger.log("attempting to send events request $eventsRequest")

    val eventResponse = eventBridgeClient.putEvents(eventsRequest)
    logger.log("event response $eventResponse")
  }

}