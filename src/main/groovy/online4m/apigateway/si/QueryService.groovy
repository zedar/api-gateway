package online4m.apigateway.si

import java.util.UUID
import javax.inject.Inject

import groovy.util.logging.Slf4j
import groovy.json.JsonSlurper

import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisConnectionException

import online4m.apigateway.ds.JedisDS

@Slf4j
class QueryService {
  // jedisDS - reference to Redis data source connection pool
  private final JedisDS jedisDS

  @Inject
  QueryService(JedisDS jedisDS) {
    this.jedisDS = jedisDS
  }

  /**
   *  Convert uuid as string to UUID class and call main getResponse()
   *  @param uuid - string representation of UUID
   */
  Response getResponse(String suuid) {
    if (!suuid) {
      return new Response(false, "SI_ERR_MISSING_UUID", "Missing service call unique identifier (UUID)")
    }
    UUID uuid = UUID.fromString(suuid)
    return getResponse(uuid)
  }

  /**
   *  Query for current response from external service call.
   *  If it was sync call, then return its response.
   *  It is was async call, then 
   *      if async response is present - return it
   *      if it is not present yet - return ack with link to query for async response
   *  @param uuid - unique identifier of request
   */
  Response getResponse(UUID uuid) {
    Jedis jedis
    try {
      if (!jedisDS || !jedisDS.isOn()) {
        return new Response(false, "SI_ERR_DATA_SOURCE_OFF", "Data source that keeps responses is not available.")
      }
      jedis = jedisDS.getResource()
      String r = jedis.hget("request:${uuid}", "response")
      String ra = jedis.hget("request:${uuid}", "responseAsync")
      log.debug("RESPONSE: ${r}")
      log.debug("RESPONSE ASYNC: ${ra}")
      jedisDS.returnResource(jedis)
      jedis = null

      def slurper = new JsonSlurper()
      Map data = slurper.parseText(ra ?: r)
      return Response.build(data)
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
        jedis = null
      }
    }
  }
}
