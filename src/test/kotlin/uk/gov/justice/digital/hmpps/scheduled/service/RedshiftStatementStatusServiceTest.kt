package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementRequest
import software.amazon.awssdk.services.redshiftdata.model.DescribeStatementResponse
import software.amazon.awssdk.services.redshiftdata.model.StatusString
import uk.gov.justice.digital.hmpps.scheduled.service.RedshiftStatementStatusService.Companion.QUERY_ABORTED
import uk.gov.justice.digital.hmpps.scheduled.service.RedshiftStatementStatusService.Companion.QUERY_FAILED
import uk.gov.justice.digital.hmpps.scheduled.service.RedshiftStatementStatusService.Companion.QUERY_FINISHED

class RedshiftStatementStatusServiceTest{

  val redshiftDataClient = mock<RedshiftDataClient>()
  val logger = mock<LambdaLogger>()

  val redshiftProperties = RedshiftProperties(
    s3location = "dpr-working-development/reports",
    redshiftDataApiDb = "dpr-product-definitions",
    redshiftDataApiSecretArn = "secretArn",
    redshiftDataApiClusterId = "clusterId"
  )

  val redshiftStatementStatusService = RedshiftStatementStatusService(
    redshiftDataClient,
    redshiftProperties
  )

  val execId = "1234"
  @Test
  fun `Failed query status throws StatementExecutionException with error from redshift`() {

    val describeResponse = mock<DescribeStatementResponse>()

    val error = "error from redshift"

    val statusString = mock<StatusString>()

    whenever(describeResponse.status()).doReturn(statusString)
    whenever(describeResponse.error()).doReturn(error)
    whenever(statusString.toString()).doReturn(QUERY_FAILED)

    whenever(redshiftDataClient.describeStatement(any<DescribeStatementRequest>())).doReturn(describeResponse)

    val exception = assertThrows<StatementExecutionException>{
      redshiftStatementStatusService.waitForQueryToComplete(execId, logger)
    }

    assertEquals(exception.error, "Query Failed to run with Error Message: $error")
  }

  @Test
  fun `Aborted query status throws StatementExecutionException`() {

    val describeResponse = mock<DescribeStatementResponse>()
    val statusString = mock<StatusString>()

    whenever(describeResponse.status()).doReturn(statusString)
    whenever(statusString.toString()).doReturn(QUERY_ABORTED)

    whenever(redshiftDataClient.describeStatement(any<DescribeStatementRequest>())).doReturn(describeResponse)

    val exception = assertThrows<StatementExecutionException>{
      redshiftStatementStatusService.waitForQueryToComplete(execId, logger)
    }

    assertEquals(exception.error, "Query was cancelled.")
  }

  @Test
  fun `Finished query status returns with correct status`() {

    val describeResponse = mock<DescribeStatementResponse>()
    val statusString = mock<StatusString>()

    whenever(describeResponse.status()).doReturn(statusString)
    whenever(statusString.toString()).doReturn(QUERY_FINISHED)

    whenever(redshiftDataClient.describeStatement(any<DescribeStatementRequest>())).doReturn(describeResponse)

    val actual = redshiftStatementStatusService.waitForQueryToComplete(execId, logger)

    assertEquals(actual, QUERY_FINISHED)
  }
}