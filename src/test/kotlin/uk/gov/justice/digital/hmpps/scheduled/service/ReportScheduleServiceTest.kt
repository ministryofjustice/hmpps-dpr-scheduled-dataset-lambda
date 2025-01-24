package uk.gov.justice.digital.hmpps.scheduled.service

import org.junit.jupiter.api.Assertions.assertEquals
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


}