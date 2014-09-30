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
It uses [*Gradle*](http://www.gradle.org) build subsystem and tends to follow [HAL - Hypertext Application Language](http://stateless.co/hal_specification.html).

API Gateway, as microservice, is used by [online4m.com](https://www.online4m.com/online4m/info/howItWorks#howitworks) - pragmatic way to develop workflow driven applications.

# API specification

## Endpoints

### /api

Lists all available APIs

**Method:** GET
**Content-Type:** application/json or application/hal+json

Example call:

    $ curl -X GET -H "Accept: application/hal+json" http://localhost:5050/api

or
    $ curl -X GET -H "Accept: application/json" http://localhost:5050/api

Response:

    {
      "_links": {
          "self": {
              "href": "http://localhost:5050/api"
          },
          "call-api": {
              "href": "http://localhost:5050/api/call",
              "title": "Call external API"
          },
          "health-checks": {
              "href": "http://localhost:5050/api/health-checks",
              "title": "Run all health checks"
          },
          "health-check-named": {
              "href": "http://localhost:5050/api/call/health-check/:name",
              "templated": true,
              "title": "Available: apigateway"
          }
      }
    }

### /api/call

Call external API.

**Method:** POST
**Content-Type:** JSON
**Input Data Format:**

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

Run all health checks: 

* online4m_api_gateway_caller_service_health_check

# Run Tests

## Prerequisites

Unit tests use stubs provided by [mountebank](http://www.mbtest.org) - really nice and practical framework.

In order to make them working install node.js together with npm package manager. I recommend to use [Node version manager](https://github.com/creationix/nvm).

    $ curl https://raw.githubusercontent.com/creationix/nvm/v0.17.0/install.sh | bash
    $ nvm install v0.11.8
    $ curl https://www.npmjs.org/install.sh | sh

Install mountebank globally:

    $ npm install -g mountebank --production

After that mountebank server should be available with command:

    $ mb

## Tests

Please look at each groovy class from test folder. 
Some of them, especially for functional testing with some real services, are annotated with @Ignore annotation (feature of [Spock](https://code.google.com/p/spock/) BDD testing framework).
Remove or comment it in order to run them while testing.

Run tests with command:

    $ ./gradlew test

The *test* task automatically runs *mountebank* server in the background. Posts stubs configuration.

When *test* is finished it is finalized with closing *mountebank* server.

Following tasks from *build.gradle* do the job:

    startMounteBank - start mountebank server with *mb* shell command
    initMounteBank  - initialize stubs configuration with *./src/test/groovy/online4m/apigateway/si/test/imposter.json* file.
    testFinished    - kill spwaned processes attached to mountebank ports

# TODO:

* Add ASYNC calls with response callbacks and storing responses in local storage
* Add EVENT async calls without waiting for response

# Project structure

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
