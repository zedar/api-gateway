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

class DeleteApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "Delete existing resource and get HTTP 204"() {
    given:
    def inputJson = [
      method: "DELETE",
      mode: "SYNC",
      format: "JSON",
      url: "http://localhost:4545/delete_cust",
      data: [
        id: "1001"
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.headers.set("Accept", "application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    def httpResp = post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with (r) {
      success == true
      errorCode == "0"
      statusCode == 200
    }
  }
}
