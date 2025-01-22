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

  val scheduledDataset = Dataset(
    id = "dataset1",
    name ="DataSet 1",
    datasource ="DS1",
    schedule = "0 15 10 ? * *",
    query = ""
  )

  val nonScheduledDataset = Dataset(
    id = "dataset2",
    name ="DataSet 2 no schedule",
    datasource ="DS2",
    query = ""
  )

  val futureScheduledDataset = Dataset(
    id = "dataset3",
    name ="DataSet 3",
    datasource ="DS3",
    schedule = "0 15 11 ? * *",
    query = ""
  )

  val report1 = Report(
    id = "report1",
    name = "Report 1",
    dataset = "\$ref:${scheduledDataset.id}"
  )

  val report2 =  Report(
    id = "report2",
    name = "Report 2",
    dataset = "\$ref:${nonScheduledDataset.id}"
  )

  val report3 =  Report(
    id = "report3",
    name = "Report 3",
    dataset = "\$ref:${futureScheduledDataset.id}"
  )

  val datasource = Datasource(
    id = "123",
    name = "testDatasource",
    database = "DIGITAL_PRISON_REPORTING",
    catalog = "nomis"
  )
  val productDefinition = ProductDefinition(
    id = "123",
    name = "testReport",
    datasource = listOf(
      datasource
    ),
    dataset = listOf(
      scheduledDataset,
      nonScheduledDataset,
      futureScheduledDataset
    ),
    report = listOf(
      report1,
      report2,
      report3
    )
  )
}