package online4m.apigateway.si.test

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

@Ignore
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
      _links
      _links.size() > 0
      _links.self.href ==~ /.+\/api$/
    }
  }

  def "Get API endpoints in HAL+JSON format"() {
    given:
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Accept", "application/hal+json")
    }
    get("api")

    then:
    response.headers.get("Content-Type") == "application/hal+json"

    def r = json.parseText(response.body.text)
    with(r) {
      _links
      _links.size() > 0
      _links.self.href ==~ /.+\/api$/
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
    with (r) {
      _links
      _links.size() > 0
      _links[0].self.href ==~ /.+\/api$/
    }
  }
}

