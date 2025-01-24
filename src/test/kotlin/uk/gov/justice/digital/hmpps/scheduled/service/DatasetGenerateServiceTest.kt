package uk.gov.justice.digital.hmpps.scheduled.service

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport
import uk.gov.justice.digital.hmpps.scheduled.model.generateNewExternalTableId
import java.util.*

class DatasetGenerateServiceTest {

  val redshiftDataClient = mock<RedshiftDataClient>()
  val redshiftProperties = mock<RedshiftProperties>()
  val datasetGenerateService = DatasetGenerateService(
    redshiftDataClient,
    redshiftProperties
  )

  @Test
  fun `Find generate table id based on product definition id and dataset id`() {

    val dataSet = DatasetWithReport(
      dataset = scheduledDataset,
      datasource = datasource,
      productDefinitionId = productDefinition.id,
      report = report1
    )

    val actual = dataSet.generateNewExternalTableId()

    val id = dataSet.productDefinitionId + ":" + dataSet.dataset.id
    val encoded = "_" + Base64.getEncoder().encodeToString(id.toByteArray())

    assertEquals(encoded, actual)
  }
}