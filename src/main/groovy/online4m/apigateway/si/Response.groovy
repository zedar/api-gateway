package online4m.apigateway.si

import groovy.transform.ToString
import groovy.transform.TupleConstructor

@ToString @TupleConstructor
class Response {
  Boolean success = true
  String  errorCode = "0"
  String  errorDescr = ""
  Object  data
}
