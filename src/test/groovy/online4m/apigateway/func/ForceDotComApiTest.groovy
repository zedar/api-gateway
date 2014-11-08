package online4m.apigateway.func

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
import spock.lang.Shared

class ForceDocComApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  @Shared Properties envProps

  // run before the first feature method
  // Intialization of common resources for all feature methods
  def setupSpec() {
    envProps = new Properties()
    envProps.load(getClass().getClassLoader().getResourceAsStream("force.com.properties"))
  }

  def "Force.com resource object lifecycle"() {
    given:
    def json = new JsonSlurper()
    def accessToken
    def instanceUrl
    def custId

    when: "get access token"
    def req = [
      method: "POST",
      mode:   "SYNC",
      format: "URLENC",
      url:    "https://login.salesforce.com/services/oauth2/token",
      data: [
        grant_type:     "password",
        client_id:      "${envProps.customer_key}",
        client_secret:  "${envProps.customer_secret}",
        username:       "${envProps.username}",
        password:       "${envProps.password}"
      ]
    ]
    requestSpec { RequestSpec reqSpec ->
      reqSpec.body.text(JsonOutput.toJson(req))
    }
    post("api/invoke")
    def resp = json.parseText(response.body.text)

    then: "access token is active"
    with(resp) {
      success == true
      statusCode == 200
      data.access_token
      data.token_type == "Bearer"
      data.instance_url
    }

    when:
    accessToken = resp.data.access_token
    instanceUrl = resp.data.instance_url

    then:
    accessToken
    instanceUrl

    when: "create new account"
    req = [
      method: "POST",
      mode:   "SYNC",
      format: "JSON",
      url:    "${instanceUrl}/services/data/v32.0/sobjects/Account/",
      headers: [
        "Authorization":  "Bearer ${accessToken}"
      ],
      data: [
        Name: "Test company name 1"
      ]
    ]
    requestSpec { RequestSpec reqSpec ->
      reqSpec.body.text(JsonOutput.toJson(req))
    }
    post("api/invoke")
    resp = json.parseText(response.body.text)

    then: "new account id is given"
    with(resp) {
      success == true
      errorCode == "0"
      statusCode == 201
      data.id
      data.success == true
    }

    when:
    custId = resp.data.id

    then:
    custId

    when: "update account properties"
    req = [
      method: "PATCH",
      mode:   "SYNC",
      format: "JSON",
      url:    "${instanceUrl}/services/data/v32.0/sobjects/Account/${custId}",
      headers: [
        "Authorization":  "Bearer ${accessToken}"
      ],
      data: [
        Name:         "New Test Company Name 1",
        BillingCity:  "Warsaw"
      ]
    ]
    requestSpec { RequestSpec reqSpec ->
      reqSpec.body.text(JsonOutput.toJson(req))
    }
    post("api/invoke")
    resp = json.parseText(response.body.text)


    then: "no error and empty content is returned"
    with(resp) {
      success == true
      errorCode == "0"
      statusCode == 204
      data == null
    }

    when: "get account properties"
    req = [
      method: "GET",
      mode:   "SYNC",
      format: "JSON",
      url:    "${instanceUrl}/services/data/v32.0/sobjects/Account/${custId}",
      headers: [
        "Authorization":  "Bearer ${accessToken}"
      ],
      data: [
        fields: "Name,BillingCity"
      ]
    ]
    requestSpec { RequestSpec reqSpec ->
      reqSpec.body.text(JsonOutput.toJson(req))
    }
    post("api/invoke")
    resp = json.parseText(response.body.text)

    then: "account should have updated properties"
    with(resp) {
      success == true
      errorCode == "0"
      statusCode == 200
      data.Name == "New Test Company Name 1"
      data.BillingCity == "Warsaw"
    }

    when: "delete account"
    req = [
      method: "DELETE",
      mode:   "SYNC",
      format: "JSON",
      url:    "${instanceUrl}/services/data/v32.0/sobjects/Account/${custId}",
      headers: [
        "Authorization":  "Bearer ${accessToken}"
      ]
    ]
    requestSpec { RequestSpec reqSpec ->
      reqSpec.body.text(JsonOutput.toJson(req))
    }
    post("api/invoke")
    resp = json.parseText(response.body.text)
    
    then: "account is deleted and no content returned"
    with(resp) {
      success == true
      errorCode == "0"
      statusCode == 204
      !data
    }

    when: "get not existing account"
    req = [
      method: "GET",
      mode:   "SYNC",
      format: "JSON",
      url:    "${instanceUrl}/services/data/v32.0/sobjects/Account/${custId}",
      headers: [
        "Authorization":  "Bearer ${accessToken}"
      ],
      data: [
        fields: "Name,BillingCity"
      ]
    ]
    requestSpec { RequestSpec reqSpec ->
      reqSpec.body.text(JsonOutput.toJson(req))
    }
    post("api/invoke")
    resp = json.parseText(response.body.text)

    println(JsonOutput.prettyPrint(JsonOutput.toJson(resp)))

    then: "account does not exists"
    with(resp) {
      success == false
      errorCode == "HTTP_ERR_404"
      statusCode == 404
    }
  }
}
