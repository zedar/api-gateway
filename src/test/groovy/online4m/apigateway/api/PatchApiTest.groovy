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

class PatchApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "Patch exisiting resource without content returned (HTTP 204)"() {
    given: "request as patch"
    def inputJson = [
      method: "PATCH",
      mode: "SYNC",
      format: "JSON",
      url: "http://localhost:4545/patch_json_cust_no_content",
      data: [
        mobile: "048900300200",
        status: "ACTIVE"
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec request ->
      request.body.type("application/json")
      request.headers.set("Accept", "application/json")
      request.body.text(JsonOutput.toJson(inputJson))
    }
    def httpResp = post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == true
      errorCode == "0"
      statusCode == 204
      data == null
    }
  }
}
