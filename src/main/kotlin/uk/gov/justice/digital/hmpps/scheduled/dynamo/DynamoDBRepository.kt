package uk.gov.justice.digital.hmpps.scheduled.dynamo

import com.google.gson.Gson
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import uk.gov.justice.digital.hmpps.scheduled.model.ProductDefinition

class DynamoDBRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: DynamoDbProductDefinitionProperties = DynamoDbProductDefinitionProperties(),
  private val gson: Gson = Gson(),
) {

  fun findReportsWithSchedule(): List<ProductDefinition> {
    return dynamoDbClient.scan(withFieldName("schedule")).items()
      ?.map { gson.fromJson(it[properties.definitionFieldName]!!.s(), ProductDefinition::class.java) }
      ?: emptyList()
  }

  private fun withFieldName(fieldName: String): ScanRequest {

    return ScanRequest.builder()
      .tableName(properties.tableName)
      .filterExpression("contains(#definition,:val)")
      .expressionAttributeNames(
        mapOf(
          "#definition" to properties.definitionFieldName,
        ),
      )
      .expressionAttributeValues(
        mapOf(
          ":val" to AttributeValue.fromS("\"${fieldName}\""),
        ),
      ).build()
  }

  fun findReportsByFileName(fileName: String): List<ProductDefinition> {
    return dynamoDbClient.scan(withScanRequest(fileName)).items()
      ?.map { gson.fromJson(it[properties.definitionFieldName]!!.s(), ProductDefinition::class.java) }
      ?: emptyList()
  }

  private fun withScanRequest(fileName: String): ScanRequest {

    return ScanRequest.builder()
      .tableName(properties.tableName)
      .filterExpression("contains(#file_name,:val)")
      .expressionAttributeNames(
        mapOf(
          "#file_name" to properties.fileNameFieldName,
        ),
      )
      .expressionAttributeValues(
        mapOf(
          ":val" to AttributeValue.fromS(fileName),
        ),
      ).build()
  }

}