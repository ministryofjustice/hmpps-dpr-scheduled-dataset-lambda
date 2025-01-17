package uk.gov.justice.digital.hmpps.scheduled.lambda

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import kotlinx.coroutines.runBlocking
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDBRepository
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDbProductDefinitionProperties
import uk.gov.justice.digital.hmpps.scheduled.service.ReportScheduleService

class ReportSchedulerLambda : RequestHandler<MutableMap<String, Any>, String> {

  private var reportSchedulingService: ReportScheduleService? = null

  init {
    val dynamoDBRepository = DynamoDBRepository(
      properties = DynamoDbProductDefinitionProperties(),
      dynamoDbClient = DynamoDbClient { region = "eu-west-2" }
    )
    reportSchedulingService = ReportScheduleService(
      dynamoDBRepository
    )
  }

  override fun handleRequest(input: MutableMap<String, Any>?, context: Context?): String = runBlocking{

    if (context != null) {
      val logger = context.logger
      logger.log("Started report scheduler", LogLevel.INFO)

      reportSchedulingService!!.findReports(logger)

      logger.log("Finished report scheduler", LogLevel.INFO)
    }

    return@runBlocking ""
  }

}
