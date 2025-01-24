package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.redshiftdata.RedshiftDataClient
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport
import uk.gov.justice.digital.hmpps.scheduled.model.generateNewExternalTableId
import java.util.*

class DatasetGenerateServiceTest {

  val redshiftDataClient = mock<RedshiftDataClient>()
  val redshiftProperties = RedshiftProperties(
    s3location = "dpr-working-development/reports",
    redshiftDataApiDb = "dpr-product-definitions",
    redshiftDataApiSecretArn = "secretArn",
    redshiftDataApiClusterId = "clusterId"
  )

  val datasetGenerateService = DatasetGenerateService(
    redshiftDataClient,
    redshiftProperties
  )

  val logger =  mock<LambdaLogger>()

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

    val actualEncoded = actual.substring(1).toByteArray()
    val actualDecoded = Base64.getDecoder().decode(actualEncoded)
    assertEquals(id, String(actualDecoded))
  }

  @Test
  fun `Generate final query from dataset`() {

    val datasetQuery = "SELECT prisoner.number FROM datamart.prisoner_profile as prisoner WHERE prisoner.active = 'Y'"

    val query = datasetGenerateService.generateFinalQuery("_123", datasetQuery)

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