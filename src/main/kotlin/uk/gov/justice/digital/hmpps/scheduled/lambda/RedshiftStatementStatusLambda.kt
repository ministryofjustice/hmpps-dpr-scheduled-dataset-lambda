package uk.gov.justice.digital.hmpps.scheduled.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import uk.gov.justice.digital.hmpps.scheduled.service.RedshiftProperties
import uk.gov.justice.digital.hmpps.scheduled.service.RedshiftStatementStatusService

data class EventBridgeEvent(
  val payload: Map<String, Any>,
) {
  val detail: Map<String, Any> by payload
  val source: String by payload
  val detailType: String = payload.getOrDefault("detail-type", "") as String
}

class RedshiftStatementStatusLambda : RequestHandler<Map<String, Any>, String> {

  private var redshiftStatementStatusService: RedshiftStatementStatusService? = null

  init {
    val redshiftDataClient = RedshiftDataClient.builder().region(Region.EU_WEST_2).build()
    val redshiftProperties = RedshiftProperties(
      redshiftDataApiClusterId = System.getenv("CLUSTER_ID"),
      redshiftDataApiDb = System.getenv("DB_NAME"),
      redshiftDataApiSecretArn = System.getenv("CREDENTIAL_SECRET_ARN"),
    )

    redshiftStatementStatusService = RedshiftStatementStatusService(
      redshiftDataClient = redshiftDataClient,
      redshiftProperties = redshiftProperties,
    )
  }

  override fun handleRequest(payload: Map<String, Any>, context: Context?): String {

    if (context != null) {
      val logger = context.logger
      logger.log("RedshiftStatementStatusLambda", LogLevel.INFO)

      logger.log("received event $payload")

      redshiftStatementStatusService!!.processEvent(EventBridgeEvent(payload), logger)

      logger.log("RedshiftStatementStatusLambda", LogLevel.INFO)
    }

    return ""
  }
}