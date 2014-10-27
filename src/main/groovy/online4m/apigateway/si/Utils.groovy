package online4m.apigateway.si

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import groovy.json.JsonOutput

import groovy.util.slurpersupport.NodeChild

class Utils {
  final static Logger log = LoggerFactory.getLogger(Utils.class)

  private static final Set simpleTypes = new HashSet([
    Boolean.class,
    Character.class,
    Byte.class,
    Short.class,
    Integer.class,
    Long.class,
    Float.class,
    Double.class,
    Void.class,
    String.class
  ])

  static boolean isSimpleType(Class clazz) {
    simpleTypes.contains(clazz)
  }

  /**
   *  Normalize json object to contains JSON known and compatible data types
   */

  /**
   *  Extract query attributes from url and inputData
   *  @param url - URL given by caller. After this method call url must have query set to null
   *  @param inputData - map of attributes and its values. If attribute is complex (Map, List) it is automatically skipped
   */
  static Map buildQueryAttributesMap(URL url, Map inputData) {
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
    log.debug("URI QUERY: ${queryMap}")
    return queryMap
  }

  /**
   *  Build request headers.
   *  @param requestHeaders - map of headers
   *  @param headersToSet - map of headers to set
   */
  static HashMap buildRequestHeaders(Map requestHeaders, Map headersToSet) {
    log.debug("BUILD REQ HEADERS: ${requestHeaders}, ${headersToSet}")
    if (!requestHeaders || !headersToSet) {
      return requestHeaders
    }
    headersToSet?.each{ key, val ->
      log.debug("SETTING HEADER: ${key}, ${val}")
      if (key instanceof String && Utils.isSimpleType(val.getClass())) {
        requestHeaders[key] = val
      }
    }

    return requestHeaders
  }

  /**
   *  Generate xml string with groovy MarkupBuilder
   *
   *  @param data - either Map or List
   */
  static String buildXmlString(Object data) {
    if (!data) {
      return null
    }
    // build body's XML recursivelly
    // declare xmlBuilder closure before its definition to be visible inside it content
    def xmlbuilder
    xmlbuilder = { c ->
      if (c instanceof Map) {
        c.each{ key, value ->
          if (Utils.isSimpleType(value.getClass())) {
            "$key"("$value")
          }
          else {
            "$key" {
              xmlbuilder(value)
            }
          }
        }
      }
      else if (c instanceof List) {
        c.each{value ->
          xmlbuilder(value)
        }
      }
    }
    
    def xmlString = new StreamingMarkupBuilder().bind {
      xmlbuilder.delegate = delegate
      xmlbuilder(data)
    }.toString()

    return xmlString
  }

  /**
   *  Normalize xml response to JSON compatible object/entity
   *  @param node - xml root node to convert to json compatible groovy object/entity
   */
  static Object buildJsonEntity(NodeChild xml) {
    if (!xml) {
      return null
    }
    
    def jsonBuilder
    jsonBuilder = { node ->
      def childNodes = node.childNodes()
      if (!childNodes.hasNext()) {
        return node.text()
      }
      else {
        def map = [:]
        childNodes.each {
          map[it.name()] = jsonBuilder(it)
        }
        return map
      }
    }

    def jsonEntity = [
      (xml.name()): jsonBuilder(xml)
    ]

    log.debug("RESPONSE jsonEntity: ${jsonEntity.toString()}")
    log.debug("RESPONSE TO JSON: ${JsonOutput.toJson(jsonEntity)}")
  
    return jsonEntity
  }
}
