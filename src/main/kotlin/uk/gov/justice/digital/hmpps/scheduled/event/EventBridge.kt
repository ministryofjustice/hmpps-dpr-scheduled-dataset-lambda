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
    const val RULE_NAME = "dpr-dataset-scheduled-test-rule"
    const val EVENT_BUS_NAME = "dpr-event-bus"
    const val TARGET_LAMBDA_ARN = "arn:aws:lambda:eu-west-2:771283872747:function:dpr-generate-dataset-function-test"
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

  fun createRule(logger: LambdaLogger) {

    val eventRule = """{
      "source": ["$RULE_SOURCE"],
      "detail-type": ["$RULE_DETAIL_TYPE_DATASET"]
      }
    """.trimIndent()

    val ruleRequest = PutRuleRequest.builder()
      .name(RULE_NAME)
      .eventBusName(EVENT_BUS_NAME)
      .eventPattern(eventRule)
      .description("A test rule to route events to another lambda")
      .build()

    val ruleResponse: PutRuleResponse = eventBridgeClient.putRule(ruleRequest)
    logger.log("rule response $ruleResponse")

    val lambdaArn = TARGET_LAMBDA_ARN
    val lambdaTarget = Target.builder()
      .arn(lambdaArn)
      .id("targetId")
      .build()

    val targetsRequest: PutTargetsRequest = PutTargetsRequest.builder()
      .eventBusName(EVENT_BUS_NAME)
      .rule(RULE_NAME)
      .targets(lambdaTarget)
      .build()

    eventBridgeClient.putTargets(targetsRequest)
  }

  fun listBuses(logger: LambdaLogger) {
    try {
      val busesRequest = ListEventBusesRequest.builder()
        .limit(10)
        .build()

      val response: ListEventBusesResponse = eventBridgeClient.listEventBuses(busesRequest)
      val buses: List<EventBus> = response.eventBuses()
      for (bus in buses) {
        logger.log("The name of the event bus is: " + bus.name())
        logger.log("The ARN of the event bus is: " + bus.arn())
      }
    } catch (e: EventBridgeException) {
      logger.log(e.awsErrorDetails().errorMessage())
    }
  }
}