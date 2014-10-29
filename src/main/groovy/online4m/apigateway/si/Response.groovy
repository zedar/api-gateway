package online4m.apigateway.si

import java.util.UUID

import groovy.util.logging.Slf4j
import groovy.transform.ToString
import groovy.transform.TupleConstructor

@ToString @TupleConstructor @Slf4j
class Response {
  Boolean success = true
  String  errorCode = "0"
  String  errorDescr = ""
  // statusCode - HTTP status code for external API call
  Integer statusCode = 200
  // id - correlation UUID. Equals to request.uuid
  //        has to be private, because overriden set and get
  UUID id
  // data - JSON object with serialized output of external API call
  Object  data
  // href - link to GET itself
  String  href = ""
  // links - map of links to related entities
  Map links = [:]

  void setId(String sid) {
    this.id = UUID.fromString(sid)
  }

  static Response build(Map data) {
    Response response = new Response()
    data.inject(response) {res, key, value ->
      if (res.hasProperty(key)) {
        res."${key}" = value
      }
      return res
    }
    return response
  }
}
