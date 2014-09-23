API Gateway - unified access to REST or Web Services
-----------------------------

API Gateway could play a role of single entry point for invocation of diverse services, either REST or WebServices.
Unifies a way services are called. 
Primary goals are: 

* all requests have to be handled asynchronously (no thread blocking);
* it is REST API itself;
* support diverse invocation modes: SYNC, ASYNC, EVENT;
* distributed as microservice, defined in Sam Newmans's book *Building Microservices*:
  * is small and focused on doing one thing well
  * is a seperate, independent process
  * communicates via language agnostic API
  * is highly decoupled

API Gateway is [*ratpack*](http://www.ratpack.io) based project. 
All classes are written in [*Groovy*](http://groovy.codehaus.org).
It uses [*Gradle*](http://www.gradle.org) build subsystem.

# API specification

## Endpoints

### api/call

Call external API.

**Method:** POST
**Content-Type:** JSON
**Data Format:**

    {
      "method": "GET|POST|PUT|DELETE",
      "mode":   "SYNC|ASYNC|EVENT",
      "format": "JSON|XML",
      "url":    "URI OF EXTERNAL ENDPOINT",
      "data":   JSON
    }

Example HipChat API call:

    $ curl -X POST -H "Content-Type: application/json" -d '{"method": "GET", "mode": "SYNC", "format": "JSON", "url": "https://api.hipchat.com/v2/room/online4m.com/history/latest?auth_token=YOUR_TOKEN", "data": {"max-results": {"l": 10}}}' -i http://localhost:5050/api/call

### api/health-checks

Run all health checks and return their values.

**Method:** GET

### api/health-check/:name

Run health check defined by the given :name.

**Method:** GET

Supported health checks: 

* online4m_api_gateway_caller_service_health_check

In this project you get:

* A Gradle build file with pre-built Gradle wrapper
* A tiny home page at src/ratpack/templates/index.html (it's a template)
* A routing file at src/ratpack/ratpack.groovy
* Reloading enabled in build.gradle
* A standard project structure:

    <proj>
      |
      +- src
          |
          +- ratpack
          |     |
          |     +- ratpack.groovy
          |     +- ratpack.properties
          |     +- public          // Static assets in here
          |          |
          |          +- images
          |          +- lib
          |          +- scripts
          |          +- styles
          |
          +- main
          |   |
          |   +- groovy
                   |
                   +- // App classes in here!
          |
          +- test
              |
              +- groovy
                   |
                   +- // Spock tests in here!

That's it! You can start the basic app with

    ./gradlew run

but it's up to you to add the bells, whistles, and meat of the application.
