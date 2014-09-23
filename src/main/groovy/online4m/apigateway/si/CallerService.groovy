package online4m.apigateway.si

import groovy.util.logging.*

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import groovy.json.JsonOutput

@Slf4j
class CallerService {

  Response invoke(String bodyText) {
    try {
      def slurper = new JsonSlurper()
      def data = slurper.parseText(bodyText)
      log.debug("INPUT DATA TYPE: ${data?.getClass()}")
      log.debug("INPUT DATA: ${data}")
      Response response = validate(data)
      if (response?.success) {
        response = invoke(Request.build(data))
        log.debug("RESULT DATA: ${response}")
      }
      else if (!response) {
        response = new Response()
        response.with {
          (success, errorCode, errorDescr) = [false, "SI_ERR_EXCEPTION", "Unexpected result from request validation"]
        }
      }
      return response
    }
    catch (IllegalArgumentException ex) {
      ex.printStackTrace()
      return new Response(false, "SI_EXP_ILLEGAL_ARGUMENT", "Exception: ${ex.getMessage()}")
    }
    catch (Exception ex) {
      ex.printStackTrace()
      return new Response(false, "SI_EXCEPTION", "Exception: ${ex.getMessage()}")
    }
  }

  Response validate(Map data) {
    if (!(data instanceof Map)) {
      return new Response(false, "SI_ERR_WRONG_INPUT", "Wrong input data format")
    }
    if (!data.method || !data.mode || !data.format || !data.url) {
      def missingAttrs = []
      data.method ?: missingAttrs.add("method")
      data.mode ?: missingAttrs.add("mode")
      data.format ?: missingAttrs.add("format")
      data.url ?: missingAttrs.add("url")
      log.debug("MISSING ATTRS: ${missingAttrs}")
      return new Response(false, "SI_ERR_MISSING_ATTRS", "Missing input attributes: ${missingAttrs}")
    }
    return new Response(true)
  }

  Response invoke(Request request) {
    log.debug("REQUEST: ${request}")

    if (request.method == RequestMethod.GET && request.format == RequestFormat.JSON) {
      return invoke_GetJson(request.url, request.data)
    }
    else if (request.method == RequestMethod.POST && request.format == RequestFormat.JSON) {
      return invoke_PostJson(request.url, request.data)
    }
    else {
      Response resp = new Response()
      resp.with {
        (success, errorCode, errorDescr) = [false, "SI_ERR_NOT_SUPPORTED_INVOCATION", "Not supported invocation mode"]
      }
      return resp
    }
  }

  Response invoke_GetJson(URL url, Map inputData) {
    // example: http://groovy.codehaus.org/HTTP+Builder
    def http = new HTTPBuilder(url)
    def result = http.request(GET, JSON) { req ->
      headers.Accept = "application/json"
      def queryMap = [:]
      inputData?.each{key, val ->
        if (key instanceof String && Utils.isSimpleType(val.getClass())) {
          queryMap[key] = val
        }
        else {
          log.warn("SKIPPING: key=${key}, value=${val}")
        }
      }

      if (url.getQuery()) {
        // extract query attributes from url and merge them qith queryMap
        String q = url.getQuery()
        String[] params = q.split("&")
        for (param in params) {
          String key, val
          (key, val) = param.split("=")
          queryMap[key] = val
        }
        url.set(
          url.getProtocol(), 
          url.getHost(), 
          url.getPort(), 
          url.getAuthority(), 
          url.getUserInfo(), 
          url.getPath(), 
          null,         // set null query
          url.getRef())
      }

      uri.query = queryMap
      log.debug("URI QUERY: ${queryMap}")

      response.success = { resp, json ->
        log.debug("SUCCESS: STATUSCODE=${resp.statusLine.statusCode}, json=${json}")
        log.debug("RESP JSON class: ${json.getClass()}")
        // convert JsonObject to Map interface
        Map jsonMap = json
        Response r = new Response()
        r.with {
          (success, data) = [true, jsonMap]
        }
        return r
      }

      response.failure = { resp ->
        log.debug("FAILURE: STATUSCODE=${resp.statusLine.statusCode}, ${resp.statusLine.reasonPhrase}")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr) = [false, "HTTP_ERR_${resp.statusLine.statusCode}", "${resp.statusLine.reasonPhrase}"]
        }
        return r
      }
    }

    if (result) {
      log.debug("RESPONSE: ${result}")
      return result
    }
    else {
      Response r = new Response()
      r.with {
        (success, errorCode, errorDescr) = [true, "SI_ERR_REST_CALL_UNDEFINED", "Response from REST call is undefined"]
      }
      return r
    }
  }

  Response invoke_PostJson(URL url, Map inputData) {
    def http = new HTTPBuilder(url)
    def result = http.request(POST, JSON) { req ->
      headers.Accept = "application/json"
      body = inputData
      
      response.success = { resp, json ->
        Map jsonMap = json
        Response r = new Response()
        r.with {
          (success, data) = [true, jsonMap]
        }
        return r
      }

      response.failure = { resp -> 
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr) = [false, "HTTP_ERR_${resp.statusLine.statusCode}", "${resp.statusLine.reasonPhrase}"]
        }
        return r
      }
    }

    if (result) {
      return result
    }
    else {
      Response r = new Response()
      r.with {
        (success, errorCode, errorDescr) = [true, "SI_ERR_REST_CALL_UNDEFINED", "Response from REST call is undefined"]
      }
      return r
    }
  }
}
