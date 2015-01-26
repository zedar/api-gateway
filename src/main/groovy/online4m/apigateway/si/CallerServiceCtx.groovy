package online4m.apigateway.si

import java.net.URI
import java.util.concurrent.atomic.AtomicReference

import groovy.transform.ToString
import groovy.transform.TupleConstructor

@ToString @TupleConstructor
class CallerServiceCtx {
  private AtomicReference<URI> serverUri = new AtomicReference<URI>(null)
  // serverUrl - protocol, host name, port number
  private AtomicReference<String> serverUrl = new AtomicReference<String>(null)

  CallerServiceCtx setServerUri(URI uri) {
    this.serverUri.compareAndSet(null, uri)
    this.serverUrl.compareAndSet(null, uri.toString())
    return this
  }

  String getServerUrl() {
    return this.serverUrl.get()
  }

  String getHost() {
    return this.serverUri.get()?.getHost()
  }

  String getPort() {
    return this.serverUri.get()?.getPort()
  }
}
