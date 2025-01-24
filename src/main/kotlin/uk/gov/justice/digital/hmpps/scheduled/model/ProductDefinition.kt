package uk.gov.justice.digital.hmpps.scheduled.model

import java.util.*

interface hasSchedule {
  val schedule: String?
}

data class Datasource(
  val id: String,
  val name: String,
  val database: String,
  val catalog: String
)

data class Report(
  val id: String,
  val name: String,
  val dataset: String,
  override val schedule: String?  = null,
) : hasSchedule

data class Dataset(
  val id: String,
  val name: String,
  val datasource: String,
  val query: String,
  override val schedule: String? = null,
) : hasSchedule

data class ProductDefinition(
  val id: String,
  val name: String,
  val datasource: List<Datasource> = emptyList(),
  val dataset: List<Dataset> = emptyList(),
  val report: List<Report> = emptyList(),
)

data class DatasetWithReport(
  val dataset: Dataset,
  val productDefinitionId: String,
  val report: Report?,
  val datasource: Datasource,
)

fun DatasetWithReport.generateNewExternalTableId() : String {
  val id = this.productDefinitionId + ":" + this.dataset.id
  return "_" + Base64.getEncoder().encodeToString(id.toByteArray())
}