package online4m.apigateway.ds

import groovy.util.logging.*

import javax.inject.Inject

import ratpack.launch.LaunchConfig

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

/**
 *  Redist data store for requests and responses.
 */
@Slf4j
class JedisDS {
  private final LaunchConfig  launchConfig
  private final boolean       on
  private final JedisPool     jedisPool

  @Inject JedisDS(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig
    log.debug("other.redis.on=${this.launchConfig.getOther("redis.on", "false")}")
    log.debug("other.redis.host=${this.launchConfig.getOther("redis.host", "redis-host-undefined")}")
    log.debug("other.redis.port=${this.launchConfig.getOther("redis.port", "redis-port-undefined")}")
    this.on = this.launchConfig.getOther("redis.on", "false").toBoolean()
    if (this.on) {
      JedisPoolConfig config = new JedisPoolConfig()
      // Test whether connection is dead when connection
      // retrieval method is called
      config.setTestOnBorrow(true)
      // Test whether connection is dead when returning a
      // connection to the pool
      config.setTestOnReturn(true)
      this.jedisPool = new JedisPool(
          config, 
          this.launchConfig.getOther("redis.host", "localhost"), 
          this.launchConfig.getOther("redis.port", "6379")?.toInteger())
    }
  }

  boolean isOn() {
    return this.on
  }

  /**
   *  Get connection to Redis. From the pool.
   */
  Jedis getResource() {
    if (isOn()) {
      return jedisPool.getResource()
    }
    else {
      return null
    }
  }

  /**
   *  Return connectin to the pool.
   */
  void returnResource(Jedis jedis) {
    if (isOn()) {
      jedisPool.returnResource(jedis)
    }
  }
}
