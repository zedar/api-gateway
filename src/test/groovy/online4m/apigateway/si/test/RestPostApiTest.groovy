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
    def r = json.parseText(response.body.text)
    with(r) {
      success == true
      errorCode == "0"
      data != null
      data.id == "1"
    }
  }

  def "Post XML synchronously with success"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "XML",
      url: "http://localhost:4545/post_xml",
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
      data != null
      println "data: ${data}, ${data.customer.getClass()}"
      data instanceof Map
      data.customer instanceof List
      data.customer.size() == 2
      data.customer[0].id == "101"
      data.customer[1].id == "102"
    }
  }

  def "Post XML with list synchronously and get success"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "XML",
      url: "http://localhost:4545/post_xml_2",
      data: [
        customer: [
          ids: [
            [id: "1"],
            [id: "2"]
          ]
        ]
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
      data != null
      println "data: ${data}, ${data.customer.getClass()}"
      data instanceof Map
      data.customer.address.street == "Alpha"
    }
  }

  def "POST URLENC with API json output and get success"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "URLENC",
      url: "http://localhost:4545/post_urlenc.json",
      data: [
        "attr_1": "attr1val",
        "attr_2": "attr2val",
        customer: [
          ids: [
            [id: "1"],
            [id: "2"]
          ]
        ]
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
      data != null
      println "data: ${data}, ${data.customer.getClass()}"
      data instanceof Map
      data.customer.address.street == "Alpha"
    }
  }
  
  def "POST URLENC with API xml output and get success"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "URLENC",
      url: "http://localhost:4545/post_urlenc.xml",
      data: [
        "attr_1": "attr1val",
        "attr_2": "attr2val",
        customer: [
          ids: [
            [id: "1"],
            [id: "2"]
          ]
        ]
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
      data != null
      println "data: ${data}, ${data.customer.getClass()}"
      data instanceof Map
      data.customer.address.street == "Beta"
    }
  }

  def "POST URLENC with API unknown output and fail"() {
    given:
    def inputJson = [
      method: "POST",
      mode: "SYNC",
      format: "URLENC",
      url: "http://localhost:4545/post_urlenc.unknown",
      data: [
        "attr_1": "attr1val",
        "attr_2": "attr2val",
        customer: [
          ids: [
            [id: "1"],
            [id: "2"]
          ]
        ]
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
      success == false
      errorCode == "SI_UNSUPPORTED_API_CONTENT_TYPE"
    }
  }
}
