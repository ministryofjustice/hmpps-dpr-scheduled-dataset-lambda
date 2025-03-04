package uk.gov.justice.digital.hmpps.scheduled.dynamo

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.google.gson.Gson
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import uk.gov.justice.digital.hmpps.scheduled.model.ProductDefinition
import uk.gov.justice.digital.hmpps.scheduled.model.ProductDefinitionWithCategory

class DynamoDBRepository(
  private val dynamoDbClient: DynamoDbClient,
  private val properties: DynamoDbProductDefinitionProperties = DynamoDbProductDefinitionProperties(),
  private val gson: Gson = Gson(),
) {

  fun findReportById(reportId: String, categoryId: String, logger: LambdaLogger): ProductDefinitionWithCategory? {

    logger.log("attempting to find report by id $reportId")

    val itemRequest = GetItemRequest.builder()
      .tableName(properties.tableName)
      .key(
        mapOf(
          properties.idFieldName to AttributeValue.fromS(reportId),
          properties.categoryFieldName to AttributeValue.fromS(categoryId)
        ),
      ).build()

    val response = dynamoDbClient.getItem(itemRequest)

    return response?.let{
      ProductDefinitionWithCategory(
        definition = gson.fromJson(it.item()[properties.definitionFieldName]!!.s(), ProductDefinition::class.java),
        category = it.item()[properties.categoryFieldName]!!.s()
      )
    }
  }

  fun findReportsWithSchedule(logger: LambdaLogger): List<ProductDefinitionWithCategory> {

    val results = doQuery(
      logger = logger
    )
    return results
      .map {
        it.log(logger)
        ProductDefinitionWithCategory(
          definition =  gson.fromJson(it[properties.definitionFieldName]!!.s(), ProductDefinition::class.java),
          category = it[properties.categoryFieldName]!!.s()
        )
      }
  }

  fun doQuery(results: MutableList<Map<String, AttributeValue>> = mutableListOf(),
              lastKey: Map<String, AttributeValue> = emptyMap(),
              logger: LambdaLogger) : List<Map<String, AttributeValue>> {

    val response = dynamoDbClient.scan(withScanRequest(lastKey))

    logger.log("find dpds response =" + response.toString()  )

    if (response.hasItems()) {
      results.addAll(response.items())
    }
    if (response.hasLastEvaluatedKey()) {
      return doQuery(results, response.lastEvaluatedKey(), logger)
    }
    return results
  }

  private fun withScanRequest(lastKey: Map<String, AttributeValue> = emptyMap()): ScanRequest {

    val scanBuilder =  ScanRequest.builder()
      .tableName(properties.tableName)
      .filterExpression("#scheduled = :val")
      .limit(500)
      .indexName(properties.categoryIndexName)
      .expressionAttributeNames(
        mapOf(
          "#scheduled" to properties.scheduleFieldName,
        ),
      )
      .expressionAttributeValues(
        mapOf(
          ":val" to AttributeValue.fromS("true"),
        ),
      )

    if (lastKey.isNotEmpty()) {
      scanBuilder.exclusiveStartKey(lastKey)
    }

    return scanBuilder.build()
  }

  fun Map<String, AttributeValue>.log(logger: LambdaLogger) {
    logger.log("found id:=" + this[properties.idFieldName] + ", category=" + this[properties.categoryFieldName] +  ", fileName=" + this[properties.fileNameFieldName])
  }
}