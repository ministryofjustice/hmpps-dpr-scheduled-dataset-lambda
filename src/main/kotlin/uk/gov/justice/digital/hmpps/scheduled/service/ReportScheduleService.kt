package uk.gov.justice.digital.hmpps.scheduled.service

import com.amazonaws.services.lambda.runtime.LambdaLogger
import org.quartz.CronExpression
import uk.gov.justice.digital.hmpps.scheduled.dynamo.DynamoDBRepository
import uk.gov.justice.digital.hmpps.scheduled.event.EventBridge
import uk.gov.justice.digital.hmpps.scheduled.model.*
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class ReportScheduleService(
  private val productDefinitionService: ProductDefinitionService,
  private val clock: Clock = Clock.systemDefaultZone(),
  private val eventBridge: EventBridge
) {
  companion object {
    const val SCHEMA_REF_PREFIX = "\$ref:"
  }

  fun processProductDefinitions(logger: LambdaLogger) {
    //FIND
    val productDefinitions = productDefinitionService.findReportsWithSchedule(logger)
    logger.log("found product definitions " + productDefinitions.size)

    if (productDefinitions.isNotEmpty()) {

      //SCHEDULE
      val scheduledDataSet = extractDatasetsToBeScheduled(productDefinitions)
      logger.log("following datasets due to be scheduled " + scheduledDataSet)

      //GENERATE data sets
      scheduledDataSet.map { scheduled ->
        if (scheduled.dataset.datasource == "datamart") {

          val event = DatasetGenerateEvent(
            datasetId = scheduled.dataset.id,
            productDefinitionId = scheduled.productDefinitionId,
            category = scheduled.category
          )

          logger.log("attempting to send dataset event " + event)
          eventBridge.sendDatasetEvent(event, logger)
        } else {
          logger.log("definition ${scheduled.productDefinitionId},  dataset ${scheduled.dataset.id}, has datasource ${scheduled.dataset.datasource} not currently supported")
        }
      }
    }
  }

  fun testSendEvent(logger: LambdaLogger) {

    //create rule
    //eventBridge.createRule(logger)

    val productDefinitions = productDefinitionService.findReportsWithSchedule(logger)
    logger.log("found definitions " + productDefinitions.size)

    if (productDefinitions.isNotEmpty()) {
      productDefinitions.map { productDefinition ->
        logger.log("found definitions id " + productDefinition.definition.id + ", report name= " + productDefinition.definition.name + ", datasource=" + productDefinition.definition.datasource)
      }

      //test PROCESS first one
      val productDefinition = productDefinitions.first()

      val flattenDataSet = productDefinitionService.flattenDataset(productDefinition, productDefinition.definition.dataset).first()

      val event = DatasetGenerateEvent(
        datasetId = flattenDataSet.dataset.id,
        productDefinitionId = flattenDataSet.productDefinitionId,
        category = flattenDataSet.category
      )

      logger.log("atttempting to send dataset event " + event)
      eventBridge.sendDatasetEvent(event, logger)
    }
  }

  fun extractDatasetsToBeScheduled(productDefs: List<ProductDefinitionWithCategory>): List<DatasetWithReport> {
    return productDefs.map { filterDatasetToBeScheduled(it) }.flatMap { it }
  }

  fun filterDatasetToBeScheduled(productDefinition: ProductDefinitionWithCategory): List<DatasetWithReport> {

    val dataSets = productDefinition.definition.dataset
      .filter { entity -> entity.hasSchedule() && shouldBeScheduled(entity.schedule!!) }

    return productDefinitionService.flattenDataset(productDefinition, dataSets)
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