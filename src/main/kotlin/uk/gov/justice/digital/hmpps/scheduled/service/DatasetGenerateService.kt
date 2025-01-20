package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport
import uk.gov.justice.digital.hmpps.scheduled.model.Datasource
import uk.gov.justice.digital.hmpps.scheduled.service.DatasetGenerateServiceAthena.Companion.DATASET_
import java.util.*

data class StatementExecutionResponse(
  val tableId: String,
  val executionId: String,
)

data class RedshiftProperties(
  val redshiftDataApiClusterId: String,
  val redshiftDataApiDb: String,
  val redshiftDataApiSecretArn: String,
  val s3location: String = "dpr-working-development/reports",
)

class DatasetGenerateService (
  private val redshiftDataClient: RedshiftDataClient,
  private val redshiftProperties: RedshiftProperties,
) {

  fun generateDataset(datasetWithReport: DatasetWithReport, logger: LambdaLogger): StatementExecutionResponse {
    //will need to look at using report id / dataset id so that this can be referenced dynamically
    val tableId = generateNewExternalTableId()
    logger.log("generated tableId " + tableId)
    val finalQuery = """
          CREATE EXTERNAL TABLE reports.$tableId 
          STORED AS parquet 
          LOCATION 's3://${redshiftProperties.s3location}/$tableId/' 
          AS ( 
          ${
      buildFinalQuery(
        datasetQuery = buildDatasetQuery(datasetWithReport.dataset.query),
      )
    }
          );
        
    """.trimIndent()

    logger.log("attempting to execute final query " + finalQuery)



    return executeQueryAsync(datasetWithReport.datasource, tableId, finalQuery)
  }

  fun executeQueryAsync(
    datasource: Datasource,
    tableId: String,
    query: String,
  ): StatementExecutionResponse {
    val statementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(redshiftProperties.redshiftDataApiClusterId)
      .database(redshiftProperties.redshiftDataApiDb)
      .secretArn(redshiftProperties.redshiftDataApiSecretArn)
      .sql(query)
      .build()

    val response: ExecuteStatementResponse = redshiftDataClient.executeStatement(statementRequest)
    return StatementExecutionResponse(tableId, response.id())
  }

  protected fun buildFinalQuery(
    datasetQuery: String
  ): String {
    val query = listOf(datasetQuery, ).joinToString(",")
    return query
  }

  fun buildDatasetQuery(query: String) = """WITH $DATASET_ AS ($query)"""

  fun generateNewExternalTableId(): String {
    return "_" + UUID.randomUUID().toString().replace("-", "_")
  }
}