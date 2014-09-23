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
  JSON, XML
}

@ToString @TupleConstructor
class Request {
  RequestMethod method
  RequestMode   mode
  RequestFormat format
  URL           url
  Object        data

  static Request build(Map data) {
    Request req = new Request()
    req.method = data.method as RequestMethod
    req.mode = data.mode as RequestMode
    req.format = data.format as RequestFormat
    req.url = data.url.toURL()
    req.data = data.data ?: []
    return req
  }
}
