package uk.gov.justice.digital.hmpps.scheduled.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDBRepository
import uk.gov.justice.digital.hmpps.scheduled.model.*
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ReportScheduleServiceTest {

  val dynamoDBRepository = mock<DynamoDBRepository>()
  val datasetGenerateService = mock<DatasetGenerateService>()

  val asOfDate = LocalDateTime.of(2024,12,9, 10,0,0)

  val reportScheduleService = ReportScheduleService(
    dynamoDBRepository = dynamoDBRepository,
    datasetGenerateService = datasetGenerateService,
    clock = Clock.fixed(asOfDate.toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
  )

  @Test
  fun `Find upcoming scheduled datasets from reports`() {

    val datasets = reportScheduleService.filterDatasetToBeScheduled(productDefinition)

    val expected = listOf(
      DatasetWithReport(
        dataset = scheduledDataset,
        datasource = datasource,
        productDefinitionId = productDefinition.id,
        report = report1
      )
    )

    assertEquals(expected, datasets)
  }

  @Test
  fun `Test Valid Cron expression at 9 59 not to be scheduled`() {
    val everyDayAt909 = "0 59 9 ? * MON-FRI"
    assertFalse(reportScheduleService.shouldBeScheduled(everyDayAt909))
  }

  @Test
  fun `Test Valid Cron expression at 10 01 to be scheduled`() {
    val everyDayAt909 = "0 01 10 ? * MON-FRI"
    assertTrue(reportScheduleService.shouldBeScheduled(everyDayAt909))
  }
}