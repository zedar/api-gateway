package online4m.apigateway.si

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

@ToString @TupleConstructor
class Request {
  RequestMethod method
  RequestMode   mode
  RequestFormat format
  URL           url
  Map           headers
  Object        data

  static Request build(Map data) {
    Request req = new Request()
    req.method = data.method as RequestMethod
    req.mode = data.mode as RequestMode
    req.format = data.format as RequestFormat
    req.url = data.url.toURL()
    req.headers = data.headers ?: [:]
    req.data = data.data ?: []
    return req
  }
}
