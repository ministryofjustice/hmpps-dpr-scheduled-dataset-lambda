package uk.gov.justice.digital.hmpps.scheduled.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport
import uk.gov.justice.digital.hmpps.scheduled.model.ExternalTableId
import uk.gov.justice.digital.hmpps.scheduled.model.generateNewExternalTableId
import java.util.*

class DatasetGenerateServiceTest {

  val redshiftDataClient = mock<RedshiftDataClient>()
  val productDefinitionService = mock<ProductDefinitionService>()

  val redshiftProperties = RedshiftProperties(
    s3location = "dpr-working-development/reports",
    redshiftDataApiDb = "dpr-product-definitions",
    redshiftDataApiSecretArn = "secretArn",
    redshiftDataApiClusterId = "clusterId"
  )

  val datasetGenerateService = DatasetGenerateService(
    redshiftDataClient,
    redshiftProperties,
    productDefinitionService = productDefinitionService
  )

  @Test
  fun `Find generate table id based on simple product definition id and dataset id`() {

    val dataSet = DatasetWithReport(
      category = "prod",
      dataset = scheduledDataset,
      datasource = datasource,
      productDefinitionId = productDefinition.definition.id,
      report = reportWithUUID
    )

    val actual = dataSet.generateNewExternalTableId()

    val id = dataSet.productDefinitionId + ":" + dataSet.dataset.id
    val encoded = "_" + Base64.getEncoder().encodeToString(id.toByteArray())

    assertEquals(encoded, actual.id)

    val actualDecoded = actual.decode()
    assertEquals(dataSet.productDefinitionId, actualDecoded.first)
    assertEquals(dataSet.dataset.id, actualDecoded.second)
  }

  @Test
  fun `generate table id based on UUIDs product definition id and dataset id`() {

    val dataSet = DatasetWithReport(
      category = "prod",
      dataset = datasestWithUUID(UUID.fromString("778456ed-448e-4501-8818-38947ba64406")),
      datasource = datasource,
      productDefinitionId = productDefinition.definition.id,
      report = reportWithUUID
    )

    val actual = dataSet.generateNewExternalTableId()

    val id = dataSet.productDefinitionId + ":" + dataSet.dataset.id
    val encoded = "_" + Base64.getEncoder().encodeToString(id.toByteArray()).replace("==","__")

    assertFalse(actual.id.contains("="))

    assertEquals(encoded, actual.id)

    val actualDecoded = actual.decode()
    assertEquals(dataSet.productDefinitionId, actualDecoded.first)
    assertEquals(dataSet.dataset.id, actualDecoded.second)
  }

  @Test
  fun `Generate final query from dataset`() {

    val datasetQuery = "SELECT prisoner.number FROM datamart.prisoner_profile as prisoner WHERE prisoner.active = 'Y'"

    val query = datasetGenerateService.generateFinalQuery(ExternalTableId("_123"), datasetQuery)

    val expected ="""
      DROP TABLE IF EXISTS reports._123; 
      CREATE EXTERNAL TABLE reports._123
      STORED AS parquet 
      LOCATION 's3://dpr-working-development/reports/_123/' 
      AS ( 
      WITH dataset_ AS (${datasetQuery}) SELECT * FROM dataset_
      );
    """.trimIndent()

    assertEquals(expected, query)
  }
}