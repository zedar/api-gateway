package online4m.apigateway.api

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

import ratpack.groovy.test.LocalScriptApplicationUnderTest
import ratpack.test.ApplicationUnderTest
import ratpack.test.http.TestHttpClients
import ratpack.test.http.TestHttpClient
import ratpack.http.client.RequestSpec
import ratpack.test.remote.RemoteControl

import spock.lang.Specification
import spock.lang.Ignore

class RequestApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "Invoke API and check if input request is registered"() {
    given:
    def inputJson = [
      method: "GET",
      mode:   "SYNC",
      format: "JSON",
      url:    "http://localhost:4545/get_sync_json",
      data: [
        req_attr1: "alpha",
        req_attr2: "beta"
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Accept", "application/json")
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    def receivedResponse = post("api/invoke")
    def rr = json.parseText(receivedResponse.body.text)
    assert rr.success
    assert rr.links
    assert rr.links.request.href
    assert rr.id
    def rrId = rr.id
    get(rr.links.request.href)

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      mode == "SYNC"
      format == "JSON"
      url == "http://localhost:4545/get_sync_json"
      id == rr.id
    }
  }

  def "Invoke API and check if result response is registered"() {
    given:
    def inputJson = [
      method: "GET",
      mode:   "SYNC",
      format: "JSON",
      url:    "http://localhost:4545/get_sync_json",
      data: [
        req_attr1: "alpha",
        req_attr2: "beta"
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Accept", "application/json")
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    def receivedResponse = post("api/invoke")
    def rr = json.parseText(receivedResponse.body.text)
    assert rr.success
    assert rr.href
    assert rr.links
    assert rr.links.request.href
    assert rr.id
    get(rr.href)

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success
      errorCode == "0"
      id == rr.id
      href == rr.href
      links
      links.request.href == rr.links.request.href
    }
  }
}
