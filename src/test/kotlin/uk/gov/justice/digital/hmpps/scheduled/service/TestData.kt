package uk.gov.justice.digital.hmpps.scheduled.service

import uk.gov.justice.digital.hmpps.scheduled.model.*
import java.util.*

val scheduledDataset = Dataset(
  id = "dataset1",
  name ="DataSet 1",
  datasource ="DS1",
  schedule = "0 15 10 ? * *",
  query = ""
)

val nonScheduledDataset = Dataset(
  id = "dataset2",
  name ="DataSet 2 no schedule",
  datasource ="DS2",
  query = ""
)

val futureScheduledDataset = Dataset(
  id = "dataset3",
  name ="DataSet 3",
  datasource ="DS3",
  schedule = "0 15 11 ? * *",
  query = ""
)

fun datasestWithUUID(uuid: UUID)  = Dataset(
  id = uuid.toString(),
  name ="DataSet 3",
  datasource ="DS3",
  schedule = "0 15 11 ? * *",
  query = ""
)

val reportWithUUID = Report(
  id = "778456ed-448e-4501-8818-38947ba64476",
  name = "Report 1",
  dataset = "\$ref:${scheduledDataset.id}"
)

val report2 =  Report(
  id = "report2",
  name = "Report 2",
  dataset = "\$ref:${nonScheduledDataset.id}"
)

val report3 =  Report(
  id = "report3",
  name = "Report 3",
  dataset = "\$ref:${futureScheduledDataset.id}"
)

val datasource = Datasource(
  id = "123",
  name = "testDatasource",
  database = "DIGITAL_PRISON_REPORTING",
  catalog = "nomis"
)
val productDefinition = ProductDefinitionWithCategory(
  category = "prod",
  definition = ProductDefinition(
  id = "123",
  name = "testReport",
  datasource = listOf(
    datasource
  ),
  dataset = listOf(
    scheduledDataset,
    nonScheduledDataset,
    futureScheduledDataset
  ),
  report = listOf(
    reportWithUUID,
    report2,
    report3
  )
)
)