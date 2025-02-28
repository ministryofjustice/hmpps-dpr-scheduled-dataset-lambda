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

data class ProductDefinitionWithCategory(
  val definition: ProductDefinition,
  val category: String
)

data class DatasetWithReport(
  val dataset: Dataset,
  val productDefinitionId: String,
  val category: String,
  val report: Report?,
  val datasource: Datasource,
)

class ExternalTableId(
  val id: String
) {
  companion object{

    fun generate(dataset: DatasetWithReport) : ExternalTableId {
      val id = "${dataset.productDefinitionId}:${dataset.dataset.id}"
      val encodedId = Base64.getEncoder().encodeToString(id .toByteArray())
      val updatedId = encodedId.replace("=", "_")
      return ExternalTableId("_${updatedId}")
    }
  }

  fun decode(): Pair<String, String> {
    val correctId = this.id.substring(1).replace("_","").toByteArray()
    val actualDecodedBytes = Base64.getDecoder().decode(correctId)
    val actualDecoded = String(actualDecodedBytes)
    val parts = actualDecoded.split(":")
    return Pair(parts.first(), parts.last())
  }
}

fun DatasetWithReport.generateNewExternalTableId() = ExternalTableId.generate(this)