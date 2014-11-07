package online4m.apigateway.si

import java.util.UUID

import javax.inject.Inject

import groovy.util.logging.*

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import groovy.util.XmlSlurper

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import ratpack.rx.RxRatpack
import ratpack.exec.ExecControl
import ratpack.exec.Execution
import ratpack.func.Action
import static ratpack.rx.RxRatpack.observe
import rx.Observable

import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisConnectionException

import online4m.apigateway.ds.JedisDS

/**
 *  This class is intended to be called as Singleton. It is threadsafe by design.
 */
@Slf4j
class CallerService {
  // jedisDS - reference to Redis data source connection pool
  private final JedisDS jedisDS
  // csCtx - common attrobutes for all service calls
  private final CallerServiceCtx csCtx
  // execControl - ratpack execution control
  private final ExecControl execControl


  @Inject 
  CallerService(ExecControl execControl, JedisDS jedisDS, CallerServiceCtx csCtx) {
    this.execControl = execControl
    this.jedisDS = jedisDS
    this.csCtx = csCtx
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
    catch (JedisConnectionException ex) {
      ex.printStackTrace()
      return new Response(false, "SI_EXP_REDIS_CONNECTION_EXCEPTION", "Exception: ${ex.getMessage()}")
    }
    catch (Exception ex) {
      ex.printStackTrace()
      return new Response(false, "SI_EXCEPTION", "Exception: ${ex.getMessage()}")
    }
  }

  /**
   *  Call external API defined by *Request* object. Assumes that request object has been validated.
   *  Supported combinations of attributes:
   *    SYNC  + GET   + JSON
   *    SYNC  + POST  + JSON
   *    SYNC  + PUT   + JSON
   *    SYNC  + GET   + XML
   *    SYNC  + POST  + XML
   *    SYNC  + PUT   + XML
   *    SYNC  + POST  + URLENC
   *    SYNC  + PUT   + URLENC
   *    SYNC  + DELETE
   *    ASYNC (with all above methods)
   *  @param request - request with attributes required to call external API
   *  @param jedis - Redis connection, unique instance for the given invocation, taken out of JedisPool
   */
  Response invoke(Request request) {
    log.debug("REQUEST: ${request.getUrl()}")

    if (jedisDS && jedisDS.isOn()) {
      Jedis jedis = jedisDS.getResource()

      jedis.hset("request:${request.id}", "request", JsonOutput.toJson(request))

      Date dt = new Date()

      jedis.zadd("request-log", dt.toTimestamp().getTime(), "request:${request.id}")

      // increment statistics
      jedis.incr("usage/year:${dt.getAt(Calendar.YEAR)}")
      jedis.incr("usage/year:${dt.getAt(Calendar.YEAR)}/month:${dt.getAt(Calendar.MONTH)+1}")
      jedis.incr("usage/year:${dt.getAt(Calendar.YEAR)}/month:${dt.getAt(Calendar.MONTH)+1}/day:${dt.getAt(Calendar.DAY_OF_MONTH)}")

      jedisDS.returnResource(jedis)
    }

    Response response = null

    if (request.mode == RequestMode.SYNC) {
      response = invokeSync(request)
    }
    else if (request.mode == RequestMode.ASYNC) {
      response = invokeAsync(request)
    }
    else {
      response = new Response()
      response.with {
        (success, errorCode, errorDescr) = [false, "SI_ERR_NOT_SUPPORTED_INVOCATION", "Not supported invocation mode", request.uuid]
      }
    }

    if (response) {
      response.id = request.id
    }

    if (jedisDS && jedisDS.isOn() && response) {
      Jedis jedis = jedisDS.getResource()
      jedis.hset("request:${request.id}", "response", JsonOutput.toJson(response))
      jedisDS.returnResource(jedis)

      String serverUrl = this.csCtx.serverUrl
      response.href = serverUrl + "/api/invoke/${response.id.toString()}/response"
      response.links["request"] = [
        href: serverUrl + "/api/invoke/${response.id.toString()}/request"
      ]
    }

    return response
  }

  /**
   *  Switch to corresponding method that invoces external API synchronously.
   *  @param request to be send to external API
   */
  private Response invokeSync(Request request) {
    Response response = null

    if (request.method == RequestMethod.GET && request.format == RequestFormat.JSON) {
      response = getJson(request.url, request.headers, request.data)
    }
    else if ([RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH].find{ it == request.method } && 
                request.format == RequestFormat.JSON) {
      response = sendJson(request.method, request.url, request.headers, request.data)
    }
    else if (request.method == RequestMethod.GET && request.format == RequestFormat.XML) {
      response = getXml(request.url, request.headers, request.data)
    }
    else if ([RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH].find { it == request.method } && 
                request.format == RequestFormat.XML) {
      response = sendXml(request.method, request.url, request.headers, request.data)
    }
    else if ([RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH].find{ it == request.method } && 
                request.format == RequestFormat.URLENC) {
      response = sendUrlEncoded(request.method, request.url, request.headers, request.data)
    }
    else if (request.method == RequestMethod.DELETE) {
      response = del(request.url, request.headers, request.data)
    }
    else {
      response = new Response()
      response.with {
        (success, errorCode, errorDescr) = [false, "SI_ERR_NOT_SUPPORTED_INVOCATION", "Not supported invocation mode", request.uuid]
      }
    }

    return response
  }

  private Response invokeAsync(Request request) {
    // Construct response data with location to get final response
    if (!this.csCtx) {
      Response r = new Response()
      r.with {
        (success, errorCode, errorDescr) = [false, "SI_NO_ACCESS_TO_PUBLIC_ADDRESS", "Unable to get access to main server's public address"]
      }
      return r
    }

    // Fork further execution
    execControl.fork(new Action<Execution>() {
      public void execute(Execution execution) throws Exception {
        ExecControl ec = execution.getControl()
        ec.blocking {
          // build context for async response
          def responseCtx = new Expando()
          responseCtx.id = request.id

          request.mode = RequestMode.SYNC
          responseCtx.response = invokeSync(request)
          
          if (!responseCtx.response.id) {
            responseCtx.response.id = request.id
          }
          return responseCtx
        }
        .then { responseCtx ->
          log.debug("POST JSON ASYNC RESPONSE: uuid: ${responseCtx.id}, response: ${responseCtx.response?.toString()}")
          // save response in redis
          // callback has an access to service context, so jedisDS is visible
          if (jedisDS.isOn()) {
            Jedis jedis = jedisDS.getResource()
            jedis.hset("request:${responseCtx.id}", "responseAsync", JsonOutput.toJson(responseCtx.response))
            jedisDS.returnResource(jedis)
          }
        }
      }
    }
    )

    // Prepare confirmation (ack) response
    Response r = new Response()
    r.with {
      (success, statusCode) = [true, 202]
    }
    return r
  }

  /**
   *  Map internal request method to groovyx.net.http.Method
   *  @param method - RequestMethod enum
   */
  private def mapMethod(RequestMethod method) {
    return groovyx.net.http.Method.valueOf(method.name())
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
          (success, data, statusCode) = [true, jsonMap, resp.statusLine.statusCode]
        }
        return r
      }

      response.failure = { resp ->
        log.debug("FAILURE: STATUSCODE=${resp.statusLine.statusCode}, ${resp.statusLine.reasonPhrase}")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr, statusCode) = [
            false, 
            "HTTP_ERR_${resp.statusLine.statusCode}", 
            "${resp.statusLine.reasonPhrase}",
            resp.statusLine.statusCode]
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

  private Response sendJson(RequestMethod method, URL url, Map headersToSet, Map inputData) {
    log.debug("method=${mapMethod(method)}")
    
    def http = new HTTPBuilder(url)
    def result = http.request(mapMethod(method) , JSON) { req ->
      headers.Accept = "application/json"
      Utils.buildRequestHeaders(headers, headersToSet)
      body = inputData

      response.success = { resp ->
        log.debug("GOT RESPONSE-CODE: ${resp.statusLine}")
        // If there is no content returned, so HTTP 204
        if (resp.statusLine.statusCode == 204) {
          Response r = new Response()
          r.with {
            (success, statusCode) = [true, resp.statusLine.statusCode]
          }
          return r
        }
        String text = resp.entity?.content?.text
        log.debug("GOT RESPONSE-SUCCESS: ${text}")
        resp.headers?.each {
          log.debug("GOT RESPONSE-HEADER: ${it.name} : ${it.value}")
        }
        String contentType = resp.headers."Content-Type"
        if (contentType?.startsWith("application/json") && text) {
          def json = new JsonSlurper().parseText(text)
          log.debug("GOT RESPONSE-PARSED: ${JsonOutput.prettyPrint(JsonOutput.toJson(json))}")
          Map jsonMap = json
          Response r = new Response()
          r.with {
            (success, data, statusCode) = [true, jsonMap, resp.statusLine.statusCode]
          }
          return r
        }
        else {
          Response r = new Response()
          r.with {
            (success, errorCode, errorDescr, statusCode) = [
              false,
              "HTTP_ERR_${resp.statusLine.statusCode}",
              "${resp.statusLine.reasonPhrase}",
              resp.statusLine.statusCode
            ]
          }
          return r
        }
      }
      
      // IMPORTANT: there is a bug in #groovylang https://jira.codehaus.org/browse/GROOVY-7132
      //  Fixed in version 2.3.8 and above. 
      //  TODO: use it if 2.3.8 is available
      /* response.success = { resp, json -> */
      /*   log.debug("SEND RESPONSE CODE: ${resp.statusLine}") */
      /*   log.debug("SEND RESPONSE SUCCESS: ${json}") */
      /*   Map jsonMap = json */
      /*   Response r = new Response() */
      /*   r.with { */
      /*     (success, data, statusCode) = [true, jsonMap, resp.statusLine.statusCode] */
      /*   } */
      /*   return r */
      /* } */

      response.failure = { resp -> 
        log.debug("SEND RESPONSE FAILURE")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr, statusCode) = [
            false, 
            "HTTP_ERR_${resp.statusLine.statusCode}", 
            "${resp.statusLine.reasonPhrase}",
            resp.statusLine.statusCode]
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
          (success, data, statusCode) = [true, Utils.buildJsonEntity(xml), resp.statusLine.statusCode]
        }
        return r
      }

      response.failure = { resp ->
        log.debug("FAILURE: STATUSCODE=${resp.statusLine.statusCode}, ${resp.statusLine.reasonPhrase}")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr, statusCode) = [
            false, 
            "HTTP_ERR_${resp.statusLine.statusCode}", 
            "${resp.statusLine.reasonPhrase}",
            resp.statusLine.statusCode]
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

  private Response sendXml(RequestMethod method, URL url, Map headersToSet, Map inputData) {
    def http = new HTTPBuilder(url)
    def result = http.request(mapMethod(method), XML) { req ->
      Utils.buildRequestHeaders(headers, headersToSet)
      body = Utils.buildXmlString(inputData)

      response.success = { resp, xml ->
        log.debug("SEND RESPONSE CODE: ${resp.statusLine}")

        // If there is no content returned, so HTTP 204
        if (resp.statusLine.statusCode == 204) {
          Response r = new Response()
          r.with {
            (success, statusCode) = [true, resp.statusLine.statusCode]
          }
          return r
        }

        Response r = new Response()
        r.with {
          (success, data, statusCode) = [true, Utils.buildJsonEntity(xml), resp.statusLine.statusCode]
        }
        return r
      }

      response.failure = { resp ->
        log.debug("POST RESPONSE FAILURE")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr, statusCode) = [
            false, 
            "HTTP_ERR_${resp.statusLine.statusCode}", 
            "${resp.statusLine.reasonPhrase}",
            resp.statusLine.statusCode]
        }
        return r
      }
    }
  }

  private Response sendUrlEncoded(RequestMethod method, URL url, Map headersToSet, Map inputData) {
    def http = new HTTPBuilder(url)
    def result = http.request(mapMethod(method)) { req ->
      Utils.buildRequestHeaders(headers, headersToSet)
      def queryMap = Utils.buildQueryAttributesMap(url, inputData)
      send URLENC, queryMap

      response.success = { resp ->
        // If there is no content returned, so HTTP 204
        if (resp.statusLine.statusCode == 204) {
          Response r = new Response()
          r.with {
            (success, statusCode) = [true, resp.statusLine.statusCode]
          }
          return r
        }

        String contentType = resp.headers."Content-Type"
        log.debug("CONTENT-TYPE: ${contentType}")
        if (contentType?.startsWith("application/json")) {
          def json = new JsonSlurper().parseText(resp.entity.content.text)
          Map jsonMap = json
          Response r = new Response()
          r.with {
            (success, data, statusCode) = [true, jsonMap, resp.statusLine.statusCode]
          }
          return r
        }
        else if (contentType?.startsWith("application/xml")) {
          def xml = new XmlSlurper().parseText(resp.entity.content.text)
          Response r = new Response()
          r.with {
            (success, data, statusCode) = [true, Utils.buildJsonEntity(xml), resp.statusLine.statusCode]
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
          (success, errorCode, errorDescr, statusCode) = [
            false, 
            "HTTP_ERR_${resp.statusLine.statusCode}", 
            "${resp.statusLine.reasonPhrase}",
            resp.statusLine.statusCode]
        }
        return r
      }
    }
  }

  private Response del(URL url, Map headersToSet, Map inputData) {
    def http = new HTTPBuilder(url)
    def result = http.request(DELETE) { req ->
      Utils.buildRequestHeaders(headers, headersToSet)
      def queryMap = Utils.buildQueryAttributesMap(url, inputData)

      uri.query = queryMap


      log.debug "HEADERS: ${headers}"
      log.debug "QUERY: ${queryMap}"

      response.success = { resp, json ->
        log.debug("SUCCESS: STATUSCODE=${resp.statusLine.statusCode}")
        Response r = new Response()
        r.with {
          (success, statusCode) = [true, resp.statusLine.statusCode]
        }
        return r
      }

      response.failure = { resp ->
        log.debug("FAILURE: STATUSCODE=${resp.statusLine.statusCode}, ${resp.statusLine.reasonPhrase}")
        Response r = new Response()
        r.with {
          (success, errorCode, errorDescr, statusCode) = [
            false, 
            "HTTP_ERR_${resp.statusLine.statusCode}", 
            "${resp.statusLine.reasonPhrase}",
            resp.statusLine.statusCode]
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
}
