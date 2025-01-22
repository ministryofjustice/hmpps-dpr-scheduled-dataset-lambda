package uk.gov.justice.digital.hmpps.scheduled.lambda


import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDBRepository
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDbProductDefinitionProperties
import uk.gov.justice.digital.hmpps.scheduled.service.DatasetGenerateService
import uk.gov.justice.digital.hmpps.scheduled.service.RedshiftProperties
import uk.gov.justice.digital.hmpps.scheduled.service.ReportScheduleService

class ReportSchedulerLambda : RequestHandler<MutableMap<String, Any>, String> {

  private var reportSchedulingService: ReportScheduleService? = null

  init {
    val dynamoDbClient = DynamoDbClient.builder().region(Region.EU_WEST_2).build()

    val dynamoDBRepository = DynamoDBRepository(
      properties = DynamoDbProductDefinitionProperties(),
      dynamoDbClient = dynamoDbClient,
    )

    val CLUSTER_ID_VAR_NAME = "CLUSTER_ID"
    val DB_NAME_VAR_NAME = "DB_NAME"
    val CREDENTIAL_SECRET_ARN_VAR_NAME = "CREDENTIAL_SECRET_ARN"

    val datasetGenerateService = DatasetGenerateService(
      redshiftDataClient = RedshiftDataClient.builder().region(Region.EU_WEST_2).build(),
      redshiftProperties = RedshiftProperties(
        redshiftDataApiClusterId = System.getenv(CLUSTER_ID_VAR_NAME),
        redshiftDataApiDb = System.getenv(DB_NAME_VAR_NAME),
        redshiftDataApiSecretArn = System.getenv(CREDENTIAL_SECRET_ARN_VAR_NAME),
      ),
    )

    reportSchedulingService = ReportScheduleService(
      dynamoDBRepository,
      datasetGenerateService,
    )
  }

  override fun handleRequest(input: MutableMap<String, Any>?, context: Context?): String {

    if (context != null) {
      val logger = context.logger
      logger.log("Started report scheduler", LogLevel.INFO)

      reportSchedulingService!!.processProductDefinitions(logger)

      logger.log("Finished report scheduler", LogLevel.INFO)
    }

    return ""
  }
}
