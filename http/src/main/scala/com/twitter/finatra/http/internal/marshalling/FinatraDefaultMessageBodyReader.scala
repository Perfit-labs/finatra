package com.twitter.finatra.http.internal.marshalling

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.inject.Injector
import com.twitter.finagle.httpx.Request
import com.twitter.finatra.conversions.string._
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.finatra.http.marshalling.DefaultMessageBodyReader
import com.twitter.finatra.json.FinatraObjectMapper
import javax.inject.{Inject, Singleton}

object FinatraDefaultMessageBodyReader {
  private val EmptyObjectNode = new ObjectNode(null)
}

@Singleton
class FinatraDefaultMessageBodyReader @Inject()(
  injector: Injector,
  objectMapper: FinatraObjectMapper)
  extends DefaultMessageBodyReader {

  /* Public */

  override def parse[T: Manifest](request: Request): T = {
    val requestAwareObjectReader = {
      val requestInjectableValues = new RequestInjectableValues(objectMapper, request, injector)
      objectMapper.reader[T].`with`(requestInjectableValues)
    }

    val length = request.contentLength.getOrElse(0L)
    if (length > 0 && isJsonEncoded(request))
      FinatraObjectMapper.parseRequestBody(request, requestAwareObjectReader)
    else
      requestAwareObjectReader.readValue(FinatraDefaultMessageBodyReader.EmptyObjectNode)
  }

  /* Private */

  private def isJsonEncoded(request: Request): Boolean = {
    request.contentType.exists { contentType =>
      contentType.startsWith("application/json")
    }
  }
}