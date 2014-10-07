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
import spock.lang.Ignore

class HipChatApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)
  
  Properties properties

  def setup() {
    
    properties = new Properties()
    properties.load(getClass().getClassLoader().getResourceAsStream("hipchat.properties"))
  }

  def "Send new notification to HipChat/online4m.com room"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "JSON",
      url: "https://api.hipchat.com/v2/room/online4m.com/notification?auth_token=${properties.auth_token}",
      data: [
        message: "Test message to online4m.com"
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
    }
  }

  def "Get latest HipChat/online4m.com history"() {
    given:
    def inputJson = [
      method: "GET",
      mode: "SYNC",
      format: "JSON",
      url: "https://api.hipchat.com/v2/room/online4m.com/history/latest?auth_token=${properties.auth_token}",
      data: [
        "max-results": 10
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
      data.items
      data.items.size() > 0
    }

  }

  def "Skip complex parameters"() {
    given:
    def inputJson = [
      method: "GET",
      mode: "SYNC",
      format: "JSON",
      url: "https://api.hipchat.com/v2/room/online4m.com/history/latest?auth_token=${properties.auth_token}",
      data: [
        "complex-attr": [
          "simple-attr": 20
        ],
        "max-results": 10
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
      data.items
      data.items.size() > 0
    }
  }
}
