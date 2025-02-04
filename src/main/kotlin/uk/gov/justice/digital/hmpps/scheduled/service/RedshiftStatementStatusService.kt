package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.ExecuteStatementResponse
import java.time.LocalDateTime

class StatementExecutionException(val error: String) : RuntimeException(error)

enum class ExecutionStatus {
  SUBMITTED,
  FAILED,
  FINISHED
}

class RedshiftStatementStatusService (
  private val redshiftDataClient: RedshiftDataClient,
  private val redshiftProperties: RedshiftProperties
) {
  companion object {
    const val WAIT_TIME = 500L
    const val TIME_OUT_SECS = 5L
    const val QUERY_STARTED = "STARTED"
    const val QUERY_FINISHED = "FINISHED"
    const val QUERY_ABORTED = "ABORTED"
    const val QUERY_FAILED = "FAILED"

    const val createStatement ="""
      CREATE TABLE IF NOT EXISTS admin.statement_execution_status
      (id bigint identity(1, 1),
      status varchar(30) not null,
      table_Id varchar(100) not null,
      execution_Id varchar(100) not null,
      error_message varchar(1000),
      created_at datetime not null default sysdate,
      updated_at datetime,
      primary key(id));
    """
  }

  fun insertWithError(response: StatementExecutionResponse, status: ExecutionStatus, error: String): String {
    return """
      INSERT INTO admin.statement_execution_status(status, table_id, execution_id, error_message ) 
      values ('${status}','${response.tableId}', '${response.executionId}', '${error}');
      """.trimIndent()
  }

  fun insert(response: StatementExecutionResponse, status: ExecutionStatus): String {
    return """
     INSERT INTO admin.statement_execution_status (status, table_id, execution_id)
      values ('${status}','${response.tableId}', '${response.executionId}');
      """.trimIndent()
  }
  fun submitStatusResponse(response: StatementExecutionResponse,
                           status: ExecutionStatus,
                           error: String? = null,
                           logger: LambdaLogger) {

    val insertStatement = error
      ?.let{ error -> insertWithError(response, status, error)}
      ?:insert(response, status)

    val createAndInsert = "$createStatement $insertStatement"
    logger.log("attempting to execute query: $createAndInsert")
    val statementResponse = executeQuery(createAndInsert)
    logger.log("got statement response from create and run ${statementResponse}")
  }

  fun andWaitToStart(response: StatementExecutionResponse, logger: LambdaLogger): ExecutionStatus {

    try {
      if (waitForQueryToStart(response.executionId, logger) == QUERY_STARTED) {
        //no error yet timed out but still processing
        submitStatusResponse(response, ExecutionStatus.SUBMITTED, null, logger)
        return ExecutionStatus.SUBMITTED
      } else {
        submitStatusResponse(response, ExecutionStatus.FINISHED, null, logger)
        return ExecutionStatus.FINISHED
      }
    }
    catch (exception: StatementExecutionException) {
      submitStatusResponse(response, ExecutionStatus.FAILED, exception.error, logger)
      return ExecutionStatus.FAILED
    }
    catch (throwable: Throwable) {
      submitStatusResponse(response, ExecutionStatus.FAILED, throwable.message, logger)
      return ExecutionStatus.FAILED
    }
  }

  fun waitForQueryToStart(executionId: String, logger: LambdaLogger): String {
    var isQueryStillRunning = true
    var latestStatus = ""
    val startTime = LocalDateTime.now().plusSeconds(TIME_OUT_SECS)
    while (isQueryStillRunning) {
      val response = checkExecutionStatus(executionId, logger)
      latestStatus = response.status().toString()
      when (latestStatus) {
        QUERY_FAILED -> {
          throw StatementExecutionException("Query Failed to run with Error Message: " + response.error())
        }
        QUERY_ABORTED -> {
          throw StatementExecutionException("Query was cancelled.")
        }
        QUERY_FINISHED -> {
          isQueryStillRunning = false
        }
        QUERY_STARTED -> {
          val now = LocalDateTime.now()
          if (now.isAfter(startTime)) {
            return latestStatus
          }
        }
        else -> {
          Thread.sleep(WAIT_TIME)
        }
      }
    }
    logger.log("returning with status $latestStatus")
    return latestStatus
  }

  fun checkExecutionStatus(statementId: String, logger: LambdaLogger): DescribeStatementResponse {
    val statementRequest = DescribeStatementRequest.builder()
      .id(statementId)
      .build()

    val describeStatementResponse = redshiftDataClient.describeStatement(statementRequest)

    return describeStatementResponse
  }

  private fun executeQuery(query: String): ExecuteStatementResponse {
    val statementRequest = ExecuteStatementRequest.builder()
      .clusterIdentifier(redshiftProperties.redshiftDataApiClusterId)
      .database(redshiftProperties.redshiftDataApiDb)
      .secretArn(redshiftProperties.redshiftDataApiSecretArn)
      .sql(query)
      .build()

    return redshiftDataClient.executeStatement(statementRequest)
  }
}