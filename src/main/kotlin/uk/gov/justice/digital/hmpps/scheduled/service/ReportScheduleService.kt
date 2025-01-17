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
  private val clock: Clock = Clock.systemDefaultZone(),
) {
  companion object {
    const val SCHEMA_REF_PREFIX = "\$ref:"
  }
  suspend fun findReports(logger: LambdaLogger) {

    //FIND
    val reports = dynamoDBRepository.findReportsByFileName("visit")
    logger.log("found reports " + reports.size)

    if (reports.isNotEmpty()) {
      logger.log(reports.first().toString())
    }

    //SCHEDULE
    val scheduled = extractDatasetsToBeScheduled(reports)
    logger.log("following datasets due to be scheduled " + scheduled)

    //PROCESS

  }

  fun flattenDataset(productDefinition: ProductDefinition, datasets: List<Dataset>): List<DatasetWithReport> {

    val reportToDataSet = productDefinition.report.map{report ->
      Pair(report.dataset.removePrefix(SCHEMA_REF_PREFIX), report)
    }.toMap()

    return datasets.filter{entity -> entity.hasSchedule() && shouldBeScheduled(entity.schedule!!)}.map{ dataset ->
      DatasetWithReport(
        dataset = dataset,
        report = reportToDataSet.get(dataset.id),
        productDefinitionId = productDefinition.id
      )
    }
  }

  fun extractDatasetsToBeScheduled(productDefs: List<ProductDefinition>) :List<DatasetWithReport> {
    return productDefs.map{filterDatasetToBeScheduled(it)}.flatMap { it }
  }

  fun filterDatasetToBeScheduled(productDefinition: ProductDefinition) :List<DatasetWithReport> {

    val dataSets = productDefinition.dataset
      .filter{entity -> entity.hasSchedule() && shouldBeScheduled(entity.schedule!!)}

    return flattenDataset(productDefinition, dataSets)
  }

  fun shouldBeScheduled(schedule: String) : Boolean {
    val cronTrigger = CronExpression(schedule)
    val now = LocalDateTime.ofInstant(clock.instant(),ZoneId.systemDefault() )
    val date = now.toDate()
    val next = cronTrigger.getNextValidTimeAfter(date)
    return if (next.toLocalDateTime() <= now.plusHours(1)) true else false
  }
}

fun hasSchedule.hasSchedule() = this.schedule!=null && this.schedule!!.isNotEmpty()
fun Date.toLocalDateTime() = LocalDateTime.ofInstant(this.toInstant(),ZoneId.systemDefault())
fun LocalDateTime.toDate() = Date.from(this.atZone(ZoneId.systemDefault()).toInstant())