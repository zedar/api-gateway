package online4m.apigateway.si

import java.util.concurrent.atomic.AtomicReference

import groovy.transform.ToString
import groovy.transform.TupleConstructor

@ToString @TupleConstructor
class CallerServiceCtx {
  // serverUrl - protocol, host name, port number
  private AtomicReference<String> serverUrl = new AtomicReference<String>(null)

  void setServerUrl(String url) {
    this.serverUrl.compareAndSet(null, url)
  }

  String getServerUrl() {
    this.serverUrl.get()
  }
}
