package online4m.apigateway.si

import java.util.UUID

import groovy.util.logging.Slf4j
import groovy.transform.ToString
import groovy.transform.TupleConstructor

enum RequestMethod {
  POST, GET, PUT, DELETE
}

enum RequestMode {
  SYNC, ASYNC, EVENT
}

enum RequestFormat {
  // Content-Type: application/json
  JSON,
  // Content-Type: application/xml
  XML, 
  // Content-Type: application/x-www-form-urlencoded
  URLENC
}

@ToString @TupleConstructor @Slf4j
class Request {
  UUID  id = UUID.randomUUID()
  RequestMethod method
  RequestMode   mode
  RequestFormat format
  URL   url 
  // HTTP request headers in form of key-value pairs. For example:
  //    "Authorization": "Bearer ACCESS_KEY"
  Map           headers
  // HTTP request data to be sent as either query attributes or body content
  Object        data
  // href - link to GET itself
  String        href = ""
  // links - map of links to related entities
  Map           links = [:]

  public void setId(String sid) {
    if (!sid) {
      this.id = UUID.randomUUID()
    }
    else
      this.id = UUID.fromString(sid)
  }

  public void setUrl(String surl) {
    this.url = surl.toURL()
  }

  static Request build(Map data) {
    Request request = new Request()
    data.inject(request) { req, key,value ->
      if (req.hasProperty(key)) {
        req."${key}" = value
      }
      return req
    }
    return request
  }
}
