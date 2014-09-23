package online4m.apigateway.si

import com.google.inject.AbstractModule
import com.google.inject.Scopes

class CallerModule extends AbstractModule {
  protected void configure() {
    bind(CallerService.class).in(Scopes.SINGLETON)
    bind(CallerServiceAsync.class).in(Scopes.SINGLETON)
  }
}
