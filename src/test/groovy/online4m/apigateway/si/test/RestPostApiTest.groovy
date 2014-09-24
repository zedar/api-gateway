package online4m.apigateway.si.test

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

import ratpack.groovy.test.LocalScriptApplicationUnderTest
import ratpack.test.ApplicationUnderTest
import ratpack.test.http.TestHttpClients
import ratpack.test.http.TestHttpClient
import ratpack.http.client.RequestSpec
import ratpack.test.remote.RemoteControl

import spock.lang.Specification

class RestPostApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "EMPTY RESPONSE"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "JSON",
      url: "http://localhost:4545/empty_post_response",
      data: [
        customer: [
          name1: "Name1",
          name2: "Name2",
          mobile: "0486009988",
          identification: [
            type: "ID",
            value: "AXX098765"
          ]
        ],
        status: "INIT"
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/call")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == true
      errorCode == "0"
      data == null
    }
  }

  def "Success: POST SYNC JSON"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "JSON",
      url: "http://localhost:4545/post_sync_json",
      data: [
        customer: [
          name1: "Name1",
          name2: "Name2",
          mobile: "0486009988",
          identification: [
            type: "ID",
            value: "AXX098765"
          ]
        ],
        status: "INIT"
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/call")

    then:
    println "RESPONSE: ${response.status?.code}"
    def r = json.parseText(response.body.text)
    with(r) {
      success == true
      errorCode == "0"
    }
  }
}
