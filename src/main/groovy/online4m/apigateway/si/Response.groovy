package online4m.apigateway.si

import java.util.UUID

import groovy.transform.ToString
import groovy.transform.TupleConstructor

@ToString @TupleConstructor
class Response {
  Boolean success = true
  String  errorCode = "0"
  String  errorDescr = ""
  // statusCode - HTTP status code for external API call
  int     statusCode = 200
  // uuid - correlation UUID. Equals to request.uuid
  UUID    uuid
  // data - JSON object with serialized output of external API call
  Object  data
}
