package uk.gov.justice.digital.hmpps.scheduled.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.logging.LogLevel
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.regionmetadata.EuWest2
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.athena.model.QueryExecutionContext
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.*
import javax.sql.DataSource

class ManageAthenaAsyncQueries : RequestHandler<MutableMap<String, Any>, String> {

  override fun handleRequest(payload: MutableMap<String, Any>, context: Context?): String {
      if (context != null) {
          val logger = context.logger
          logger.log("Athena Query Management Lambda Invoked.", LogLevel.INFO)
          logger.log("Received event $payload", LogLevel.INFO)
          val redshiftClient = RedshiftDataClient.builder()
              .region(Region.EU_WEST_2)
              .build()
//          val dataSource = DataSourceBuilder.create()
//              .url("")
//              .username("")
//              .password("")
//              .driverClassName("")
//              .build()
//          val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
//          val mapSqlParameterSource = mutableMapOf("" to "")
//          val result = jdbcTemplate
//              .queryForList(
//                  "SELECT * FROM datamart.admin.execution_manager;",
//                  MapSqlParameterSource(mapSqlParameterSource),
//              )
//          logger.log("Retrieved ${result.size} results from admin table.", LogLevel.INFO)
              val athenaClient: AthenaClient = AthenaClient.builder()
                  .region(Region.EU_WEST_2)
                  .build()
//              val database = result[0]["database"] as String
//              val catalog = result[0]["catalog"] as String
//              val query = result[0]["query"] as String
              val database = "DIGITAL_PRISON_REPORTING"
              val catalog = "nomis"
              val query = "SELECT agy_loc_id FROM OMS_OWNER.LIVING_UNITS limit 10;"
              logger.log("The query from the admin table is: $query", LogLevel.INFO)
              val queryExecutionContext = QueryExecutionContext.builder()
                  .database(database)
                  .catalog(catalog)
                  .build()
              /*
              """
                      CREATE TABLE AwsDataCatalog.reports.testingeventbridge
                      WITH (
                        format = 'PARQUET'
                      )
                      AS (
                      SELECT * FROM TABLE(system.query(query =>

                      )))
            """.trimIndent()
            */
          val startQueryExecutionRequest = StartQueryExecutionRequest.builder()
              .queryString(query)
              .queryExecutionContext(queryExecutionContext)
              .workGroup("dpr-generic-athena-workgroup")
              .build()
          logger.log("Full async query: $query", LogLevel.INFO)
          val queryExecutionId = athenaClient
              .startQueryExecution(startQueryExecutionRequest).queryExecutionId()
          logger.log("Query execution ID: $queryExecutionId", LogLevel.INFO)
          return queryExecutionId
    }
    return ""
  }

    private fun queryRedshift(database: String, query:String, redshiftDataClient: RedshiftDataClient, logger: LambdaLogger): GetStatementResultResponse {
        val statementRequest = ExecuteStatementRequest.builder()
            .clusterIdentifier("dpr-redshift-development")
            .database(database)
            .secretArn(query)
            .sql(query)
            .build()
        val executionId = redshiftDataClient.executeStatement(statementRequest).id()
        val describeStatementRequest = DescribeStatementRequest.builder()
            .id(executionId)
            .build()
        var describeStatementResponse: DescribeStatementResponse
        do {
            Thread.sleep(500)
            describeStatementResponse = redshiftDataClient.describeStatement(describeStatementRequest)
            if (describeStatementResponse.status() == StatusString.FAILED) {
                logger.log("Statement with execution ID: $executionId failed with the following error: ${describeStatementResponse.error()}",
                    LogLevel.ERROR)
                throw RuntimeException("Statement with execution ID: $executionId failed.")
            } else if (describeStatementResponse.status() == StatusString.ABORTED) {
                logger.log("Statement with execution ID: $executionId was aborted", LogLevel.ERROR)
                throw RuntimeException("Statement with execution ID: $executionId was aborted.")
            }
        }
        while (describeStatementResponse.status() != StatusString.FINISHED)
        val getStatementResultRequest = GetStatementResultRequest.builder()
            .id(executionId)
            .build()
        return redshiftDataClient.getStatementResult(getStatementResultRequest)
    }

    private fun getData(columnName: String, rowNumber: Int, getStatementResultResponse: GetStatementResultResponse): Any? {
        val columnNameToResultIndex = mutableMapOf<String, Int>()
        getStatementResultResponse.columnMetadata().forEachIndexed{ i, colMetaData -> columnNameToResultIndex[colMetaData.name()] = i}
        return getStatementResultResponse.records()[0][columnNameToResultIndex[columnName]!!]
    }
}