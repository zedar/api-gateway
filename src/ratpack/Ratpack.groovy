import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

import ratpack.server.PublicAddress

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.xml.MarkupBuilder

import groovy.json.JsonSlurper

import groovy.transform.TupleConstructor

import ratpack.registry.Registries
import ratpack.rx.RxRatpack
import ratpack.codahale.metrics.CodaHaleMetricsModule
import ratpack.perf.incl.*
import ratpack.codahale.metrics.HealthCheckHandler
import com.codahale.metrics.health.HealthCheckRegistry

import online4m.apigateway.health.CallerServiceHealthCheck

import online4m.apigateway.si.CallerModule
import online4m.apigateway.si.CallerService
import online4m.apigateway.si.CallerServiceAsync
import online4m.apigateway.si.QueryServiceAsync
import online4m.apigateway.si.Request
import online4m.apigateway.si.Response
import online4m.apigateway.si.Utils
import online4m.apigateway.ds.CallerDSModule
import online4m.apigateway.si.CallerServiceCtx
import online4m.apigateway.docs.ApiDocsHandler


final Logger log = LoggerFactory.getLogger(Ratpack.class)

ratpack {
  bindings {
    add new CodaHaleMetricsModule().metrics().jvmMetrics().healthChecks().jmx()
    bind CallerServiceHealthCheck
    add new CallerModule()
    add new CallerDSModule()

    init {
      RxRatpack.initialize()
    }
  }

  handlers {
    get {
      render groovyTemplate("index.html", title: "My Ratpack App")
    }

    handler { CallerServiceCtx csCtx, PublicAddress publicAddress ->
      // common functionality for all the other REST methods
      if (csCtx && !csCtx.serverUrl) {
        csCtx.serverUri = publicAddress.getAddress(context)
      }

      // call next() to process request
      next()
    }

    prefix("api-docs") {
      handler registry.get(ApiDocsHandler)
    }

    prefix("api") {
      // get list of available APIs - follow HAL hypertext application language conventions
      get { CallerServiceCtx csCtx ->
        redirect "api-docs"
      }

      // call reactive way - RxJava
      post("invoke") { CallerServiceAsync callerServiceAsync ->
        callerServiceAsync.invokeRx(request.body.text).single().subscribe() { Response response ->
          log.debug "BEFORE JsonOutput.toJson(response)"
          //getResponse().status(201)
          render JsonOutput.prettyPrint(JsonOutput.toJson(response))
        }
      }

      get("invoke/:id/request") { QueryServiceAsync queryService ->
        def sid = pathTokens["id"]
        queryService.getRequestRx(sid).single().subscribe() { Request request ->
          render JsonOutput.prettyPrint(JsonOutput.toJson(request))
        }
      }
      
      get("invoke/:id/response") { QueryServiceAsync queryService ->
        def sid = pathTokens["id"]
        queryService.getResponseRx(sid).single().subscribe() { Response response ->
          render JsonOutput.prettyPrint(JsonOutput.toJson(response))
        }
      }

      // call with ratpack promise
      post("invoke1") { CallerServiceAsync callerService ->
        callerService.invoke(request.body.text).then {
          render JsonOutput.toJson(it)
        }
      }

      // call with ratpack blocking code (it is running in seperate thread)
      post("invoke2") {CallerService callerService ->
        blocking {
          return callerService.invoke(request.body.text)
        }.then {
          render JsonOutput.toJson(it) // response.toString()
        }
      }

      post("invoke3") { CallerService callerService ->
        Response response = callerService.invoke(request.body.text)
        render JsonOutput.toJson(response)
      }


      // get named health check
      get("health-check/:name", new HealthCheckHandler())

      // run all health checks
      get("health-checks") { HealthCheckRegistry healthCheckRegistry ->
        render healthCheckRegistry.runHealthChecks().toString()
      }

      get("bycontent") {
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
