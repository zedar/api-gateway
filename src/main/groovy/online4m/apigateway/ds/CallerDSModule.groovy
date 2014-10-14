package online4m.apigateway.ds

import com.google.inject.AbstractModule
import com.google.inject.Scopes

class CallerDSModule extends AbstractModule {
  protected void configure() {
    bind(JedisDS.class).in(Scopes.SINGLETON)
  }
}
