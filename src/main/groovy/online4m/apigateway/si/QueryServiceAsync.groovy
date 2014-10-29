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

  Promise<Response> getResponse(String sid) {
    return execControl.blocking {
      return queryService.getResponse(sid)
    }
  }

  Observable<Response> getResponseRx(String sid) {
    return observe(execControl.blocking {
      return queryService.getResponse(sid)
    })
  }

  Promise<Request> getRequest(String sid) {
    return execControl.blocking {
      return queryService.getRequest(sid)
    }
  }

  Observable<Request> getRequestRx(String sid) {
    return observe(execControl.blocking {
      return queryService.getRequest(sid)
    })
  }
}
