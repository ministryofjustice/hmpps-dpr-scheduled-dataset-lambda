package uk.gov.justice.digital.hmpps.scheduled.event

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.google.gson.Gson
import software.amazon.awssdk.services.eventbridge.EventBridgeClient
import software.amazon.awssdk.services.eventbridge.model.*
import software.amazon.awssdk.services.eventbridge.model.Target
import uk.gov.justice.digital.hmpps.scheduled.service.StatementExecutionResponse


class EventBridge(
  val eventBridgeClient: EventBridgeClient,
  private val gson: Gson = Gson(),
) {

  companion object {
    const val RULE_SOURCE = "uk.gov.justice.digital.hmpps.scheduled.lambda.ReportSchedulerLambda"
    const val RULE_DETAIL_TYPE = "RedShiftStatementResponse"
    const val RULE_NAME = "dpr-dataset-scheduled-test-rule"
    const val EVENT_BUS_NAME = "dpr-event-bus-test"
  }

  fun send(statementResponse: StatementExecutionResponse, logger: LambdaLogger) {
    val payload = gson.toJson(statementResponse)
    logger.log("attempting to send payload $payload")
    val entry = PutEventsRequestEntry.builder()
      .source(RULE_SOURCE)
      .eventBusName(EVENT_BUS_NAME)
      .detail(payload)
      .detailType(RULE_DETAIL_TYPE)
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
      "detailType": ["$RULE_DETAIL_TYPE"]
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

    val lambdaArn = "arn:aws:lambda:eu-west-2:771283872747:function:dpr-redshift-statement-status-test"
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