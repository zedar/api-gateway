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

@Ignore
class WeatherApiTest extends Specification {
  ApplicationUnderTest aut = new LocalScriptApplicationUnderTest("other.remoteControl.enabled": "true")
  @Delegate TestHttpClient client = TestHttpClients.testHttpClient(aut)
  RemoteControl remote = new RemoteControl(aut)

  def "success with GET, SYNC, JSON"() {
    given:
    def inputJson = [
      method: "GET",
      mode: "SYNC", 
      format: "JSON", 
      url: "http://api.openweathermap.org/data/2.5/weather", 
      data: [q: "Warsaw"]]
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
      data.name == "Warsaw"
    }
  }
}
