package online4m.apigateway.si

import groovy.util.logging.*

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import groovy.util.XmlSlurper

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import groovy.json.JsonOutput

@Slf4j
class CallerService {

  /**
   *  Invoke external API. Parse input body text to json map. Validate input data.
   *  Build *Request* object and then call external API.
   *  @param bodyText - not parsed body text
   */
  Response invoke(String bodyText) {
    try {
      def slurper = new JsonSlurper()
      def data = slurper.parseText(bodyText)
      log.debug("INPUT DATA TYPE: ${data?.getClass()}")
      log.debug("INPUT DATA: ${data}")
      Response response = validate(data)
      if (response?.success) {
        response = invoke(Request.build(data))
        log.debug("INVOKE FINISHED")
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

  /**
   *  Validate input json map if all required attributes are provided.
   *  @param data - body text converted to json map
   */
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
    if (data.headers && !(data.headers instanceof Map)) {
      log.debug("Incorrect data type for 'headers' attribute")
      return new Response(false, "SI_ERR_WRONG_TYPEOF_HEADERS", 'headers attribute has to be JSON object - "headers": {}')
    }
    if (data.data && !(data.data instanceof Map)) {
      log.debug("Incorrect data type for 'data' attribute")
      return new Response(false, "SI_ERR_WRONG_TYPEOF_DATA", 'data attribute has to be JSON object - "data": {}')
    }
    return new Response(true)
  }

  /**
   *  Call external API defined by *Request* object. Assumes that request object has been validated.
   *  Supported combinations of attributes:
   *    SYNC + GET + JSON
   *    SYNC + POST + JSON
   *    SYNC + GET + XML
   *    SYNC + POST + XML
   *    SYNC + POST + URLENC
   *  @param request - request with attributes required to call external API
   */
  Response invoke(Request request) {
    log.debug("REQUEST: ${request}")

    if (request.method == RequestMethod.GET && request.format == RequestFormat.JSON) {
      return getJson(request.url, request.headers, request.data)
    }
    else if (request.method == RequestMethod.POST && request.format == RequestFormat.JSON) {
      return postJson(request.url, request.headers, request.data)
    }
    else if (request.method == RequestMethod.GET && request.format == RequestFormat.XML) {
      return getXml(request.url, request.headers, request.data)
    }
    else if (request.method == RequestMethod.POST && request.format == RequestFormat.XML) {
      return postXml(request.url, request.headers, request.data)
    }
    else if (request.method == RequestMethod.POST && request.format == RequestFormat.URLENC) {
      return postUrlEncoded(request.url, request.headers, request.data)
    }
    else {
      Response resp = new Response()
      resp.with {
        (success, errorCode, errorDescr) = [false, "SI_ERR_NOT_SUPPORTED_INVOCATION", "Not supported invocation mode"]
      }
      return resp
    }
  }

  private Response getJson(URL url, Map headersToSet, Map inputData) {
    def http = new HTTPBuilder(url)
    def result = http.request(GET, JSON) { req ->
      headers.Accept = "application/json"
      Utils.buildRequestHeaders(headers, headersToSet)
      def queryMap = Utils.buildQueryAttributesMap(url, inputData)

      uri.query = queryMap

      log.debug "HEADERS: ${headers}"
      log.debug "QUERY: ${queryMap}"

      response.success = { resp, json ->
        log.debug("SUCCESS: STATUSCODE=${resp.statusLine.statusCode}")
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

  private Response postJson(URL url, Map headersToSet, Map inputData) {
    def http = new HTTPBuilder(url)
    def result = http.request(POST, JSON) { req ->
      headers.Accept = "application/json"
      Utils.buildRequestHeaders(headers, headersToSet)
      body = inputData
      
      response.success = { resp, json ->
        log.debug("POST RESPONSE CODE: ${resp.statusLine}")
        log.debug("POST RESPONSE SUCCESS: ${json}")
        Map jsonMap = json
        Response r = new Response()
        r.with {
          (success, data) = [true, jsonMap]
        }
        return r
      }

      response.failure = { resp -> 
        log.debug("POST RESPONSE FAILURE")
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

  private Response getXml(URL url, Map headersToSet, Map inputData) {
    def http = new HTTPBuilder(url)
    def result = http.request(GET, XML) { req ->
      Utils.buildRequestHeaders(headers, headersToSet)
      def queryMap = Utils.buildQueryAttributesMap(url, inputData)

      uri.query = queryMap

      response.success = { resp, xml ->
        log.debug("GET XML RESPONSE CODE: ${resp.statusLine}")
        log.debug("GET XML RESPONSE TYPE: ${xml.getClass()}")
        Response r = new Response()
        r.with {
          (success, data) = [true, Utils.buildJsonEntity(xml)]
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

  private Response postXml(URL url, Map headersToSet, Map inputData) {
    log.debug("XML inputData: ${inputData.toString()}")
    def http = new HTTPBuilder(url)
    def result = http.request(POST, XML) { req ->
      Utils.buildRequestHeaders(headers, headersToSet)
      body = Utils.buildXmlString(inputData)

      response.success = { resp, xml ->
        log.debug("POST RESPONSE CODE: ${resp.statusLine}")

        Response r = new Response()
        r.with {
          (success, data) = [true, Utils.buildJsonEntity(xml)]
        }
        return r
      }

      response.failure = { resp ->
        log.debug("POST RESPONSE FAILURE")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr) = [false, "HTTP_ERR_${resp.statusLine.statusCode}", "${resp.statusLine.reasonPhrase}"]
        }
        return r
      }
    }
  }

  private Response postUrlEncoded(URL url, Map headersToSet, Map inputData) {
    log.debug("URLENCODED inputData: ${inputData.toString()}")
    def http = new HTTPBuilder(url)
    def result = http.request(POST) { req ->
      Utils.buildRequestHeaders(headers, headersToSet)
      def queryMap = Utils.buildQueryAttributesMap(url, inputData)
      send URLENC, queryMap

      response.success = { resp ->
        String contentType = resp.headers."Content-Type"
        log.debug("CONTENT-TYPE: ${contentType}")
        if (contentType.startsWith("application/json")) {
          def json = new JsonSlurper().parseText(resp.entity.content.text)
          Map jsonMap = json
          Response r = new Response()
          r.with {
            (success, data) = [true, jsonMap]
          }
          return r
        }
        else if (contentType.startsWith("application/xml")) {
          def xml = new XmlSlurper().parseText(resp.entity.content.text)
          Response r = new Response()
          r.with {
            (success, data) = [true, Utils.buildJsonEntity(xml)]
          }
          return r
        }
        else {
          return new Response(false, "SI_UNSUPPORTED_API_CONTENT_TYPE", "Unsupported API content type")
        }
      }

      response.failure = { resp ->
        log.debug("POST RESPONSE FAILURE")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr) = [false, "HTTP_ERR_${resp.statusLine.statusCode}", "${resp.statusLine.reasonPhrase}"]
        }
        return r
      }
    }
  }
}
