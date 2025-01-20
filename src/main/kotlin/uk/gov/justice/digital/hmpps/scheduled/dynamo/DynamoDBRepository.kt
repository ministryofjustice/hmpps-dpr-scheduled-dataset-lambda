package uk.gov.justice.digital.hmpps.scheduled.dynamo

import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import com.google.gson.Gson
import uk.gov.justice.digital.hmpps.scheduled.model.ProductDefinition

class DynamoDBRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: DynamoDbProductDefinitionProperties = DynamoDbProductDefinitionProperties(),
  private val gson: Gson = Gson()
) {

  suspend fun findReportsWithSchedule() : List<ProductDefinition> {
    return dynamoDbClient.scan(withFieldName("schedule")).items
      ?.map { gson.fromJson(it[properties.definitionFieldName]!!.asS(), ProductDefinition::class.java) }
      ?: emptyList()
  }

  private fun withFieldName(fieldName: String) : ScanRequest {
    return ScanRequest {
      tableName = properties.tableName
      filterExpression = "contains(#definition,:val)"
      expressionAttributeNames = mapOf(
        "#definition" to properties.definitionFieldName
      )
      expressionAttributeValues = mapOf(
        ":val" to AttributeValue.S("\"${fieldName}\""),
      )
    }
  }

  suspend fun findReportsByFileName(fileName: String): List<ProductDefinition> {
    return dynamoDbClient.scan(withScanRequest(fileName)).items
      ?.map { gson.fromJson(it[properties.definitionFieldName]!!.asS(), ProductDefinition::class.java) }
      ?: emptyList()
  }

  private fun withScanRequest(fileName: String) : ScanRequest {
    return ScanRequest {
      tableName = properties.tableName
      filterExpression = "contains(#file_name,:val)"
      expressionAttributeNames = mapOf(
        "#file_name" to properties.fileNameFieldName
      )
      expressionAttributeValues = mapOf(
        ":val" to AttributeValue.S(fileName)
      )
    }
  }
}