package online4m.apigateway.si.test

import java.util.regex.Pattern

import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.util.XmlSlurper

import ratpack.groovy.test.LocalScriptApplicationUnderTest
import ratpack.test.ApplicationUnderTest
import ratpack.test.http.TestHttpClients
import ratpack.test.http.TestHttpClient
import ratpack.http.client.RequestSpec
import ratpack.test.remote.RemoteControl

import online4m.apigateway.si.Utils

import spock.lang.Specification
import spock.lang.Ignore

class ApiEndpointsTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "Get API endpoints in Json format"() {
    given:
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Accept", "application/json")
    }
    get("api")

    then:
    response.headers.get("Content-Type") == "application/json"
    def r = json.parseText(response.body.text)
    with(r) {
      href ==~ /.+\/api$/
      title
      links
      links.size() == 5
      links.invoke.href ==~ /.+\/api\/invoke$/
      links.invoke.type == "api"
      links.request.href ==~  /.+\/api\/invoke\/\{id\}\/request$/
      links.request.type == "api"
      links.response.href ==~ /.+\/api\/invoke\/\{id\}\/response$/
      links.response.type == "api"
    }
  }

  def "Get API endpoints in vnd.api+json format - json-api.org"() {
    given:
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Accept", "application/vnd.api+json")
    }
    get("api")

    then:
    response.headers.get("Content-Type") == "application/vnd.api+json"

    def r = json.parseText(response.body.text)
    with(r) {
      href ==~ /.+\/api$/
      title
      links
      links.size() == 5
      links.invoke.href ==~ /.+\/api\/invoke$/
      links.invoke.type == "api"
      links.request.href ==~ /.+\/api\/invoke\/\{id\}\/request$/
      links.request.type == "api"
      links.response.href ==~ /.+\/api\/invoke\/\{id\}\/response$/
      links.response.type == "api"
    }
  }
  
  def "Get API endpoints in XML format"() {
    given:
    def xml = new XmlSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Accept", "application/xml")
    }
    get("api")

    then:
    response.headers.get("Content-Type") == "application/xml"

    def x = xml.parseText(response.body.text)
    def r = Utils.buildJsonEntity(x)
    r.api
    with (r.api) {
      href ==~ /.+\/api$/
      title
      links
      links.size() == 5
      links.invoke.href ==~ /.+\/api\/invoke$/
      links.invoke.type == "api"
      links.request.href ==~ /.+\/api\/invoke\/\{id\}\/request$/
      links.request.type == "api"
      links.response.href ==~ /.+\/api\/invoke\/\{id\}\/response$/
      links.response.type == "api"
    }
  }
}

