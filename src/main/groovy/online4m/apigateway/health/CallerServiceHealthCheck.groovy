package online4m.apigateway.health

import ratpack.codahale.metrics.NamedHealthCheck
import com.codahale.metrics.health.HealthCheck

class CallerServiceHealthCheck extends NamedHealthCheck {
  protected HealthCheck.Result check() throws Exception {
    return HealthCheck.Result.healthy()
  }

  String getName() {
    return "online4m_api_gateway_caller_service_health_check"
  }
}
