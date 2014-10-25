package online4m.apigateway.si

import javax.inject.Inject

import groovy.util.logging.Slf4j

import ratpack.exec.ExecControl
import ratpack.exec.Promise
import rx.Observable
import static ratpack.rx.RxRatpack.observe

@Slf4j
class QueryServiceAsync {
  private final ExecControl execControl
  private final QueryService  queryService

  @Inject
  QueryServiceAsync(ExecControl execControl, QueryService queryService) {
    this.execControl = execControl
    this.queryService = queryService
  }

  Promise<Response> getResponse(String suuid) {
    return execControl.blocking {
      return queryService.getResponse(suuid)
    }
  }

  Observable<Response> getResponseRx(String suuid) {
    return observe(execControl.blocking {
      return queryService.getResponse(suuid)
    })
  }

}
