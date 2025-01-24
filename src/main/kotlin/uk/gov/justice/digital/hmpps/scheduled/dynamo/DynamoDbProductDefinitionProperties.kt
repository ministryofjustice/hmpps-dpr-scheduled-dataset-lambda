package uk.gov.justice.digital.hmpps.scheduled.dynamo

class DynamoDbProductDefinitionProperties(
  val tableName: String = "dpr-data-product-definition",
  val fileNameFieldName: String = "filename",
  val idFieldName: String = "data-product-id",
  val categoryFieldName: String = "category",
  val definitionFieldName: String = "definition",
  val categoryIndexName: String = "category-index",
  val scheduleFieldName: String = "scheduled",
)