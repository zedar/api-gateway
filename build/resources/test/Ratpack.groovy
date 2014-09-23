import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.xml.MarkupBuilder

import groovy.json.JsonSlurper

import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.perf.incl.*
import ratpack.codahale.metrics.HealthCheckHandler
import com.codahale.metrics.health.HealthCheckRegistry

import online4m.apigateway.health.CallerServiceHealthCheck

import online4m.apigateway.si.CallerModule
import online4m.apigateway.si.CallerService
import online4m.apigateway.si.CallerServiceAsync
import online4m.apigateway.si.Request
import online4m.apigateway.si.Response

final Logger log = LoggerFactory.getLogger(Ratpack.class)

ratpack {
  bindings {
    add new CodaHaleMetricsModule().metrics().jvmMetrics().healthChecks().jmx()
    bind CallerServiceHealthCheck
    add new CallerModule()
  }

  handlers {
    get {
      render groovyTemplate("index.html", title: "My Ratpack App")
    }

    prefix("api") {
      handler {
        // common functionality for all the other REST methods
        
        // log HTTP header names and their values
        request.headers?.names?.each {name ->
          log.debug("HEADER ${name}, VALUES: ${request.headers?.getAll(name)}")
        }

        // call next() to process request
        next()
      }

      // get named health check
      get("health-check/:name", new HealthCheckHandler())

      // run all health checks
      get("health-checks") { HealthCheckRegistry healthCheckRegistry ->
        render healthCheckRegistry.runHealthChecks().toString()
      }

      post("call") {CallerServiceAsync callerServiceAsync ->
        callerServiceAsync.invokeRx(request.body.text).single().subscribe() { Response response ->
          render JsonOutput.toJson(response)
        }
      }
      
      post("call1") { CallerServiceAsync callerService ->
        callerService.invoke(request.body.text).then {
          render JsonOutput.toJson(it)
        }
      }

      post("call2") {CallerService callerService ->
        blocking {
          def slurper = new JsonSlurper()
          def data = slurper.parseText(request.body.text)
          log.debug("INPUT DATA TYPE: ${data?.getClass()}")
          log.debug("INPUT DATA: ${data}")
          Response response = callerService.validate(data)
          if (response?.success) {
            response = callerService.invoke(Request.build(data))
            log.debug("RESULT DATA: ${response}")
          }
          else if (!response) {
            response = new Response()
            response.with {
              (success, errorCode, errorDescr) = [false, "SI_ERR_EXCEPTION", "Unexpected result from request validation"]
            }
          }
          /* render JsonOutput.toJson(response) */
          return response
        }.then {
          render JsonOutput.toJson(it) // response.toString()
        }
      }

      get {
        byContent {
          json {
            // if HTTP header: Accept is not given then first type is returned as default
            // so json is default return format
            log.debug("RESPOND JSON")
            def builder = new JsonBuilder()
            builder.root {
              type "JSON"
            }
            render builder.toString()
          }
          xml {
            log.debug("RESPOND XML")
            def swriter = new StringWriter()
            new MarkupBuilder(swriter).root {
              type(a: "A", b: "B", "XML")
            }
            render swriter.toString()
          }
        }
      }
    }


        
    assets "public"
  }
}
