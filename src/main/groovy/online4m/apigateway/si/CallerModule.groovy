package online4m.apigateway.si

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.Provides
import com.google.inject.Singleton

// Explanation of Guice modules: https://github.com/google/guice/wiki/GettingStarted


class CallerModule extends AbstractModule {
  protected void configure() {
    bind(CallerService.class).in(Scopes.SINGLETON)
    bind(CallerServiceAsync.class).in(Scopes.SINGLETON)
    bind(QueryService.class).in(Scopes.SINGLETON)
    bind(QueryServiceAsync.class).in(Scopes.SINGLETON)
  }

  @Provides @Singleton
  CallerServiceCtx provideCtx() {
    return new CallerServiceCtx()
  }
}
