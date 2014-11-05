package online4m.apigateway.api

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

class PostAsyncApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "Success: POST ASYNC JSON"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "ASYNC",
      format: "JSON",
      url: "http://localhost:4545/post_async_json",
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
    def latch = new CountDownLatch(1)

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    def responseReceived = post("api/invoke")
    def rr = json.parseText(response.body.text)
    assert rr.success
    assert rr.errorCode == "0"
    assert rr.statusCode == 202
    assert rr.href

    // wait 4 seconds
    latch.await(4, TimeUnit.SECONDS)

    inputJson = [:]
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set("Accept", "application/json")
      requestSpec.body.type("application/json")
    }
    get(rr.href)

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == true
      errorCode == "0"
      statusCode == 201
      id == rr.id
      data != null
      data.id == "101"
    }
  }
}
