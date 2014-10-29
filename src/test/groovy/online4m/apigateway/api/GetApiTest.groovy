package online4m.apigateway.test

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

class GetApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "Missing REQUIRED: method"() {
    given:
    def inputJson = [
      mode: "SYNC",
      format: "JSON",
      url: "http://localhost:4545/get_sync_json"
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == false
      errorCode == "SI_ERR_MISSING_ATTRS"
      errorDescr ==~ /.+\[(.?)+method(.?)+\](.?)+/
    }
  }

  def "Missing REQUIRED: method, mode, format, url"() {
    given:
    def inputJson = [:]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == false
      errorCode == "SI_ERR_MISSING_ATTRS"
      errorDescr ==~ /.+\[(.?)+method(.?)+\](.?)+/
      errorDescr ==~ /.+\[(.?)+mode(.?)+\](.?)+/
      errorDescr ==~ /.+\[(.?)+format(.?)+\](.?)+/
      errorDescr ==~ /.+\[(.?)+url(.?)+\](.?)+/
    }
  }

  def "Wrong INPUT: data format"() {
    given:
    def inputJson = [
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == false
      errorCode == "SI_EXP_ILLEGAL_ARGUMENT"
      errorDescr ==~ /(.?)+Exception(.?)+/
    }
  }

  def "Wrong INPUT: invocation method"() {
    given:
    def inputJson = [
      method: "GET2",
      mode: "SYNC",
      format: "JSON",
      url: "http://localhost:4545/get_sync_json"
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == false
      errorCode == "SI_EXP_ILLEGAL_ARGUMENT"
    }
  }

  def "Success: GET SYNC JSON"() {
    given:
    def inputJson = [
      method: "GET",
      mode: "SYNC",
      format: "JSON",
      url: "http://localhost:4545/get_sync_json",
      data: [
        req_attr1: "alpha",
        req_attr2: "beta",
        req_attr3: [
          req_attr3_1: "3_1"
        ]
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == true
      errorCode == "0"
      r.data
      r.data.resp_attr1 == "beta"
      r.data.resp_attr2 == "alpha"
      r.data.resp_attr3.resp_attr3_1 == "3_1"
    }
  }

  def "Get XML with success"() {
    given:
    def inputJson = [
      method: "GET",
      mode: "SYNC",
      format: "XML",
      url: "http://localhost:4545/get_xml",
      data: [
        req_attr1: "alpha",
        req_attr2: "beta",
        req_attr3: [
          req_attr3_1: "3_1"
        ]
      ]
    ]
    def json = new JsonSlurper()

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.text(JsonOutput.toJson(inputJson))
    }
    post("api/invoke")

    then:
    def r = json.parseText(response.body.text)
    with(r) {
      success == true
      errorCode == "0"
      r.data
      r.data.customer.address.street == "Beta"
    }
  }
}
