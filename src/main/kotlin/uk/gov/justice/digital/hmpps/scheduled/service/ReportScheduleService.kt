package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.quartz.CronExpression
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDBRepository
import uk.gov.justice.digital.hmpps.scheduled.model.Dataset
import uk.gov.justice.digital.hmpps.scheduled.model.DatasetWithReport
import uk.gov.justice.digital.hmpps.scheduled.model.ProductDefinition
import uk.gov.justice.digital.hmpps.scheduled.model.hasSchedule
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class ReportScheduleService(
  private val dynamoDBRepository: DynamoDBRepository,
  private val datasetGenerateService: DatasetGenerateService,
  private val clock: Clock = Clock.systemDefaultZone(),
) {
  companion object {
    const val SCHEMA_REF_PREFIX = "\$ref:"
  }

  fun processProductDefinitions(logger: LambdaLogger) {
    //FIND
    val productDefinitions = dynamoDBRepository.findReportsWithSchedule(logger)
    logger.log("found product definitions " + productDefinitions.size)

    if (productDefinitions.isNotEmpty()) {

      //SCHEDULE
      val scheduledDataSet = extractDatasetsToBeScheduled(productDefinitions)
      logger.log("following datasets due to be scheduled " + scheduledDataSet)

      //GENERATE data sets
      scheduledDataSet.map { scheduled ->
        if (scheduled.dataset.datasource == "datamart") {
          val response = datasetGenerateService.generateDataset(scheduled, logger)
          logger.log("definition ${scheduled.productDefinitionId},  dataset ${scheduled.dataset.id}, got statement response " + response)
        } else {
          logger.log("definition ${scheduled.productDefinitionId},  dataset ${scheduled.dataset.id}, has datasource ${scheduled.dataset.datasource} not currently supported")
        }
      }
    }
  }

  /*
   * using this for testing dataset generation, will remove once finalised
   */
  fun testRun(logger: LambdaLogger) {

    //FIND
    val productDefinitions = dynamoDBRepository.findReportsWithSchedule(logger)
    logger.log("found definitions " + productDefinitions.size)

    if (productDefinitions.isNotEmpty()) {
      productDefinitions.map { productDefinition ->
        logger.log("found definitions id " + productDefinition.id + ", report name= " + productDefinition.name + ", datasource=" + productDefinition.datasource)
      }

      //test PROCESS first one
      val productDefinition = productDefinitions.first()

      val flattenDataSet = flattenDataset(productDefinition, productDefinition.dataset).first()
      logger.log("atttempting to generate dataset for " + flattenDataSet)
      val response = datasetGenerateService.generateDataset(flattenDataSet, logger)

      logger.log("got statement response " + response)

    }

    //SCHEDULE
    val scheduled = extractDatasetsToBeScheduled(productDefinitions)
    logger.log("following datasets due to be scheduled " + scheduled)
  }

  fun extractDatasetsToBeScheduled(productDefs: List<ProductDefinition>): List<DatasetWithReport> {
    return productDefs.map { filterDatasetToBeScheduled(it) }.flatMap { it }
  }

  fun flattenDataset(productDefinition: ProductDefinition, datasets: List<Dataset>): List<DatasetWithReport> {

    val reportToDataSet = productDefinition.report.map { report ->
      Pair(report.dataset.removePrefix(SCHEMA_REF_PREFIX), report)
    }.toMap()

    return datasets.map { dataset ->
      DatasetWithReport(
        dataset = dataset,
        report = reportToDataSet.get(dataset.id),
        productDefinitionId = productDefinition.id,
        datasource = productDefinition.datasource.first(),
      )
    }
  }

  fun filterDatasetToBeScheduled(productDefinition: ProductDefinition): List<DatasetWithReport> {

    val dataSets = productDefinition.dataset
      .filter { entity -> entity.hasSchedule() && shouldBeScheduled(entity.schedule!!) }

    return flattenDataset(productDefinition, dataSets)
  }

  fun shouldBeScheduled(schedule: String): Boolean {
    val cronTrigger = CronExpression(schedule)
    val now = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault())
    val date = now.toDate()
    val next = cronTrigger.getNextValidTimeAfter(date)
    return if (next.toLocalDateTime() <= now.plusHours(1)) true else false
  }
}

fun hasSchedule.hasSchedule() = this.schedule != null && this.schedule!!.isNotEmpty()
fun Date.toLocalDateTime() = LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
fun LocalDateTime.toDate() = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())