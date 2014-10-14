package online4m.apigateway.si

import com.google.inject.AbstractModule
import com.google.inject.Scopes

// Explanation of Guice modules: https://github.com/google/guice/wiki/GettingStarted


class CallerModule extends AbstractModule {
  protected void configure() {
    bind(CallerService.class).in(Scopes.SINGLETON)
    bind(CallerServiceAsync.class).in(Scopes.SINGLETON)
  }
}
