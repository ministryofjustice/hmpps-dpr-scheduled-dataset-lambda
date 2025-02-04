package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport
import uk.gov.justice.digital.hmpps.scheduled.model.Datasource
import uk.gov.justice.digital.hmpps.scheduled.model.ExternalTableId
import uk.gov.justice.digital.hmpps.scheduled.model.generateNewExternalTableId

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
  private val redshiftStatementStatusService: RedshiftStatementStatusService = RedshiftStatementStatusService(redshiftDataClient, redshiftProperties),
) {

  companion object {
    const val DATASET_ = """dataset_"""
  }

  fun generateDataset(datasetWithReport: DatasetWithReport, logger: LambdaLogger): StatementExecutionResponse {
    val tableId = datasetWithReport.generateNewExternalTableId()
    logger.log("generated tableId " + tableId)
    val finalQuery = generateFinalQuery(tableId, datasetWithReport.dataset.query)
    logger.log("attempting to execute final query " + finalQuery)

    val response = executeQueryAsync(datasetWithReport.datasource, tableId, finalQuery)
    redshiftStatementStatusService.andWaitToStart(response, logger)
    return response
  }

  fun generateDatasetAsync(datasetWithReport: DatasetWithReport, logger: LambdaLogger): StatementExecutionResponse {
    val tableId = datasetWithReport.generateNewExternalTableId()
    logger.log("generated tableId " + tableId)
    val finalQuery = generateFinalQuery(tableId, datasetWithReport.dataset.query)
    logger.log("attempting to execute final query " + finalQuery)

    return executeQueryAsync(datasetWithReport.datasource, tableId, finalQuery)
  }

  fun generateFinalQuery(tableId: ExternalTableId, datasetQuery: String) : String {
    return """
          DROP TABLE IF EXISTS reports.${tableId.id}; 
          CREATE EXTERNAL TABLE reports.${tableId.id}
          STORED AS parquet 
          LOCATION 's3://${redshiftProperties.s3location}/${tableId.id}/' 
          AS ( 
          ${
      buildFinalQuery(
        datasetQuery = buildDatasetQuery(datasetQuery),
      )
    }
          );
    """.trimIndent()
  }

  fun executeQueryAsync(
    datasource: Datasource,
    tableId: ExternalTableId,
    query: String,
  ): StatementExecutionResponse {
    val statementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(redshiftProperties.redshiftDataApiClusterId)
      .database(redshiftProperties.redshiftDataApiDb)
      .secretArn(redshiftProperties.redshiftDataApiSecretArn)
      .sql(query)
      .build()

    val response: ExecuteStatementResponse = redshiftDataClient.executeStatement(statementRequest)
    return StatementExecutionResponse(tableId.id, response.id())
  }

  protected fun buildFinalQuery(
    datasetQuery: String
  ): String {
    val query = listOf(datasetQuery, ).joinToString(",")
    return query
  }

  fun buildDatasetQuery(query: String) = """WITH $DATASET_ AS ($query) SELECT * FROM $DATASET_"""

}