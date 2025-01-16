package uk.gov.justice.digital.hmpps.scheduled.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.logging.LogLevel

class ReportSchedulerLambda : RequestHandler<MutableMap<String, Any>, String> {

  override fun handleRequest(input: MutableMap<String, Any>?, context: Context?): String {

    if (context != null) {
      val logger = context.logger
      logger.log("Started report scheduler", LogLevel.INFO)

      logger.log("Finished report scheduler", LogLevel.INFO)
    }

    return "null"
  }

}
