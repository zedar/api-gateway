package online4m.apigateway.si

import groovy.util.logging.*
import com.google.inject.Inject

// ratpack interface for performing async operations
import ratpack.exec.ExecControl
import ratpack.exec.Promise

// RxJava
import static ratpack.rx.RxRatpack.observe
import rx.Observable

@Slf4j
class CallerServiceAsync {
  private final ExecControl   execControl
  private final CallerService callerService

  @Inject CallerServiceAsync(ExecControl execControl, CallerService callerService) {
    this.execControl = execControl
    this.callerService = callerService
  }

  Promise<Response> invoke(String bodyText) {
    return execControl.blocking {
      return callerService.invoke(bodyText)
    }
  }

  Observable<Response> invokeRx(String bodyText) {
    return observe(execControl.blocking {
      return callerService.invoke(bodyText)
    })
  }

}
