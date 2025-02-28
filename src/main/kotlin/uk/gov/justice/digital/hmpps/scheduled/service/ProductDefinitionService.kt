package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDBRepository
import uk.gov.justice.digital.hmpps.scheduled.model.Dataset
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport
import uk.gov.justice.digital.hmpps.scheduled.model.ProductDefinitionWithCategory
import uk.gov.justice.digital.hmpps.scheduled.service.ReportScheduleService.Companion.SCHEMA_REF_PREFIX

class ProductDefinitionService(
  private val dynamoDBRepository: DynamoDBRepository,
) {

  fun findReportsWithSchedule(logger: LambdaLogger): List<ProductDefinitionWithCategory> {
    return dynamoDBRepository.findReportsWithSchedule(logger)
  }

  fun findReportById(reportId: String, categoryId: String, logger: LambdaLogger): ProductDefinitionWithCategory? {
    return dynamoDBRepository.findReportById(reportId, categoryId, logger)
  }

  fun flattenDataset(productDefinition: ProductDefinitionWithCategory, datasets: List<Dataset>): List<DatasetWithReport> {

    val reportToDataSet = productDefinition.definition.report.map { report ->
      Pair(report.dataset.removePrefix(SCHEMA_REF_PREFIX), report)
    }.toMap()

    return datasets.map { dataset ->
      DatasetWithReport(
        dataset = dataset,
        report = reportToDataSet.get(dataset.id),
        category = productDefinition.category,
        productDefinitionId = productDefinition.definition.id,
        datasource = productDefinition.definition.datasource.first(),
      )
    }
  }
}