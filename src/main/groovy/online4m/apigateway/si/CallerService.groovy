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

  @Inject CallerService(ExecControl execControl, JedisDS jedisDS, CallerServiceCtx csCtx) {
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
    Jedis jedis = null
    try {
      if (jedisDS.isOn()) {
        jedis = jedisDS.getResource()
      }
      
      def slurper = new JsonSlurper()
      def data = slurper.parseText(bodyText)
      log.debug("INPUT DATA TYPE: ${data?.getClass()}")
      log.debug("INPUT DATA: ${data}")
      Response response = validate(data)
      if (response?.success) {
        response = invoke(Request.build(data), jedis)
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
    finally {
      if (jedis) {
        jedisDS.returnResource(jedis)
      }
    }
  }

  /**
   *  Call external API defined by *Request* object. Assumes that request object has been validated.
   *  Supported combinations of attributes:
   *    SYNC  + GET   + JSON
   *    SYNC  + POST  + JSON
   *    SYNC  + GET   + XML
   *    SYNC  + POST  + XML
   *    SYNC  + POST  + URLENC
   *    ASYNC + POST  + JSON
   *  @param request - request with attributes required to call external API
   *  @param jedis - Redis connection, unique instance for the given invocation, taken out of JedisPool
   */
  Response invoke(Request request, Jedis jedis = null) {
    log.debug("REQUEST: ${request}")

    if (jedis) {
      jedis.hset("request:${request.uuid}", "request", JsonOutput.toJson(request))

      Date dt = new Date()

      jedis.zadd("request-log", dt.toTimestamp().getTime(), "request:${request.uuid}")

      // increment statistics
      jedis.incr("usage/year:${dt.getAt(Calendar.YEAR)}")
      jedis.incr("usage/year:${dt.getAt(Calendar.YEAR)}/month:${dt.getAt(Calendar.MONTH)+1}")
      jedis.incr("usage/year:${dt.getAt(Calendar.YEAR)}/month:${dt.getAt(Calendar.MONTH)+1}/day:${dt.getAt(Calendar.DAY_OF_MONTH)}")

    }

    Response response = null

    if (request.mode == RequestMode.SYNC && request.method == RequestMethod.GET && request.format == RequestFormat.JSON) {
      response = getJson(request.url, request.headers, request.data)
    }
    else if (request.mode == RequestMode.SYNC && request.method == RequestMethod.POST && request.format == RequestFormat.JSON) {
      response =  postJson(request.url, request.headers, request.data)
    }
    else if (request.mode == RequestMode.SYNC && request.method == RequestMethod.GET && request.format == RequestFormat.XML) {
      response = getXml(request.url, request.headers, request.data)
    }
    else if (request.mode == RequestMode.SYNC && request.method == RequestMethod.POST && request.format == RequestFormat.XML) {
      response = postXml(request.url, request.headers, request.data)
    }
    else if (request.mode == RequestMode.SYNC && request.method == RequestMethod.POST && request.format == RequestFormat.URLENC) {
      response = postUrlEncoded(request.url, request.headers, request.data)
    }
    else if (request.mode == RequestMode.ASYNC && request.method == RequestMethod.POST && request.format == RequestFormat.JSON) {
      response = postJsonAsync(request.uuid, request.url, request.headers, request.data)
    }
    else {
      response = new Response()
      response.with {
        (success, errorCode, errorDescr) = [false, "SI_ERR_NOT_SUPPORTED_INVOCATION", "Not supported invocation mode", request.uuid]
      }
    }

    if (response) {
      response.uuid = request.uuid
    }

    if (jedis && response) {
      jedis.hset("request:${request.uuid}", "response", JsonOutput.toJson(response))
    }

    return response
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
        if (contentType?.startsWith("application/json")) {
          def json = new JsonSlurper().parseText(resp.entity.content.text)
          Map jsonMap = json
          Response r = new Response()
          r.with {
            (success, data) = [true, jsonMap]
          }
          return r
        }
        else if (contentType?.startsWith("application/xml")) {
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

  private Response postJsonAsync(UUID uuid, URL url, Map headersToSet, Map inputData) {
    // Construct response data with location to get final response
    if (!this.csCtx) {
      Response r = new Response()
      r.with {
        (success, errorCode, errorDescr) = [false, "SI_NO_ACCESS_TO_PUBLIC_ADDRESS", "Unable to get access to main server's public address"]
      }
      return r
    }

    execControl.fork(new Action<Execution>() {
      public void execute(Execution execution) throws Exception {
        ExecControl ec = execution.getControl()
        ec.blocking {
          return postJson(url, headersToSet, inputData)
        }
        .then {
          log.debug("POST JSON ASYNC RESPONSE: ${it.toString()}")
        }
      }
    })
    /* observe(execControl.blocking{ */
    /*   return postJson(url, headersToSet, inputData) */
    /* }) */
    /* .single().subscribe() { Response response -> */
    /*   // store response in hash set */
    /*   log.debug("POST JSON ASYNC RESPONSE: ${response.toString()}") */
    /*   if (jedis && response) { */
    /*     jedis.hset("request:${uuid}", "asyncresponse", JsonOutput.toJson(response)) */
    /*   } */
    /* } */

    String serverUrl = this.csCtx.serverUrl
    log.debug("LAUNCHCONFIG PUB ADDR: ${serverUrl}")
    def jsonData = [
      location: serverUrl + "/api/asyncresponse/${uuid.toString()}"
    ]
    // Prepare confirmation (ack) response
    Response r = new Response()
    r.with {
      (success, statusCode, data) = [true, 202, jsonData]
    }
    return r
  }
}
