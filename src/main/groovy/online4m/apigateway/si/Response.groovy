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
  // uuid - correlation UUID. Equals to request.uuid
  //        has to be private, because overriden set and get
  private UUID uuid
  // data - JSON object with serialized output of external API call
  Object  data

  void setUuid(String suuid) {
    this.uuid = UUID.fromString(suuid)
  }

  void setUuid(UUID uuid) {
    this.uuid = uuid
  }

  static Response build(Map data) {
    Response response = new Response()
    data.inject(response) {res, key, value ->
      if (res.hasProperty(key)) {
        res."${key}" = value
      }
      return response
    }
  }
}
