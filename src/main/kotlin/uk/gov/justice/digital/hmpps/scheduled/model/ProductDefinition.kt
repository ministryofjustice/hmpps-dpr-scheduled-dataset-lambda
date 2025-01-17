package uk.gov.justice.digital.hmpps.scheduled.model

interface hasSchedule {
  val schedule: String?
}

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
  override val schedule: String? = null,
) : hasSchedule

data class ProductDefinition(
  val id: String,
  val name: String,
  val dataset: List<Dataset> = emptyList(),
  val report: List<Report> = emptyList(),
)

data class DatasetWithReport(
  val dataset: Dataset,
  val productDefinitionId: String,
  val report: Report?,
)