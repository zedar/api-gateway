API Gateway - unified access to REST or Web Services
-----------------------------

API Gateway could play a role of single entry point for invocation of diverse services, either REST or WebServices.
Unifies a way, services are called. 
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
      "method":   "GET|POST|PUT|DELETE",
      "mode":     "SYNC|ASYNC|EVENT",
      "format":   "JSON|XML|URLENC",
      "url":      "URI OF EXTERNAL ENDPOINT",
      "headers":  JSON,
      "data":     JSON
    }

**method:**

  * Currently supported are: **GET** and **POST**

**mode:**
  
  * mode=SYNC   - call API synchronously, send request and wait for response
  * mode=ASYNC  - call API asynchronously, send request and do not wait for response. Response might be avilable for caller as:
    * callback invocation
    * pull request
  * mode=EVENT  - call API asynchronously without response, send request as notification
  
**format:**

  * format=JSON sets header **Content-Type: application/json**
  * format=XML sets header **Content-Type: application/xml**
  * format=URLENC sets header **Content-Type: application/x-www-form-urlencoded**

**Important:** URLENC format makes sense only for method=POST.

**url:**

  * url of target API
    * If method==GET query parameters (after "?") are merged with simple attributes from **data** structure.

**headers:**

  * list of HTTP request headers in the form of key-value pairs
    
  "headers": {
    "Authorization": "Bearer ACCESS_TOKEN"
  }

**data:**

  * JSON either with list of query parameters or request body content.

#### Example: HipChat - get history of chats

Example HipChat API call:

    $ curl -X POST -H "Content-Type: application/json" -d '{"method": "GET", "mode": "SYNC", "format": "JSON", "url": "https://api.hipchat.com/v2/room/online4m.com/history/latest?auth_token=YOUR_TOKEN", "data": {"max-results": {"l": 10}}}' -i http://localhost:5050/api/call

#### Example: Twitter query with OAUTH authorization

Before any API call you have to register your application in twitter. By doing this you get unique client id and client secret.
These attributes are needed to ask for access token. Access token is used in all subsequent api calls.

Point your browser to [apps.twitter.com](https://apps.twitter.com), click the button *Create New App* and  register your application.

Next, request for a access token.

    $ curl -X POST -H "Content-Type: application/json" -d '{"method": "POST", "mode": "SYNC", "format": "URLENC", "url": "https://api.twitter.com/oauth2/token", "data": {"grant_type": "client_credentials", "client_id", "YOUR_APP_ID", "client_secret", "YOUR_APP_SECRET"}}' -i http://localhost:5050/api/call

As result you should get:
  
    {
      "errorCode":"0",
      "data": {
        "access_token":"ACCESS_TOKEN_URLENCODED",
        "token_type":"bearer"
      },
      "success":true
    }

Now you are ready to call, for example, twitter's search API. But now to request add headers map:

    "headers": {
      "Authorization": "Bearer ACCESS_TOKEN_URLENCODED"
    }

and invocation:

    $ curl -X POST -H "Content-Type: application/json" -d '{"method": "GET", "mode": "SYNC", "format": "JSON", "url": "https://api.twitter.com/1.1/search/tweets.json", "headers": {"Authorization": " Bearer ACCESS_TOKEN_URLENCODED"}, "data": {"q": "ratpackweb"}' -i http://localhost:5050/api/call

### api/health-checks

Run all health checks and return their values.

**Method:** GET

### api/health-check/:name

Run health check defined by the given :name.

**Method:** GET

Defined health checks:

  * *apigateway*

# Run Tests

## Prerequisites

### Mountebank - for stubbing and mocking

Unit tests use stubs provided by [mountebank](http://www.mbtest.org) - really nice and practical framework.

In order to make them working install node.js together with npm package manager. I recommend to use [Node version manager](https://github.com/creationix/nvm).

    $ curl https://raw.githubusercontent.com/creationix/nvm/v0.17.0/install.sh | bash
    $ nvm install v0.11.8
    $ curl https://www.npmjs.org/install.sh | sh

Install mountebank globally:

    $ npm install -g mountebank --production

After that mountebank server should be available with command:

    $ mb

### Redis - for requests persistance and statistics

API Gateway uses [Redis](http://redis.io) key/value store for persisting requests and their responses and collecting statistics.
There are tests that require Redis to be installed or accessible.

Install redis in your system:

    $ curl -O http://download.redis.io/releases/redis-2.8.17.tar.gz
    $ tar -xvzf redis-2.8.17.tar.gz
    $ cd redis-2.8.17.tar.gz
    $ make
    $ make install

Then in your system the following commands should be visible: redis-server (start redis server), redis-cli (start redis command line shell).

### Running dependencies

When **./gradlew test** starts it automatically sets up dependencies: *mountebank* and *redis*. There are two gradle tasks:

  * **runEnv** - starts servers required for testing:
    * **MounteBank** - external API stubs
    * **Redis** - key/value store
  * **cleanEnv** - stops servers used for testing

The assumption is that these servers are available on specific ports. If you change them please look at **stopEnv** task
in *build.gradle* file. There is table of ports in there.

## Tests

Please look at each groovy class from test folder. 
Some of them, especially for functional testing with some real services, are annotated with @Ignore annotation (feature of [Spock](https://code.google.com/p/spock/) BDD testing framework).
Remove or comment it out in order to run them while testing.

Run tests with command:

    $ ./gradlew test

Above command automatically sets up dependencies: *MounteBank* and *Redis* for testing.  
Inside *build.gradle* file there are two helpfull gradle tasks:

  * **runEnv** - starts servers required for testing:
    * **MounteBank** - external API stubs
    * **Redis** - key/value store
  * **cleanEnv** - stops servers used for testing

The assumption is that these servers are available on specific ports.  
If you change them please look at **stopEnv** task definition. There is a table of ports in there.

    task stopEnv(type: FreePorts) {
      // port: 2525 - mb (MounteBank), 
      // port: 6379 - redis-server (Redis data store)
      ports = [2525, 6379]
    }

The following tasks from *build.gradle* do the job:

    startMounteBank - start mountebank server with *mb* shell command
    initMounteBank  - initialize stubs configuration with *./src/test/groovy/online4m/apigateway/si/test/imposter.json* file.
    testFinished    - kill spwaned processes attached to mountebank ports

# Key/value data storage

API Gateway keeps highly dynamic data in key/value store - [Redis](http://redis.io/).  
It is used for:

  * statistics
  * requests and their corresponding responses
    * if mode=ASYNC, response is stored for future retrival
  * logging of top requests and responses

## Statistics

### Usage

Collecting number of requests:

  * usage/year:{yyyy}
  * usage/year:{yyyy}/month:{mm}
  * usage/year:{yyyy}/month:{mm}/day:{dd}

To get statistic value:

    $ redis-cli> get usage/year:2014

### Requests store

Every request is stored as Redis hash and has structure:

  * key: **request:UUID** - where UUID is unique ID of request
    * field: **request**, value: **request serialized to JSON**
    * field: **response**, value: **response serialized to JSON**
    * field: **aresponse**, value: **async response serialized to JSON**

If mode=ASYNC, **response** field stores first answer that is result of request registration and request sending.  
Then **aresponse** field stores final response from service call.

If mode=SYNC, **response** field stores final response from service call.

To get request and response for particular UUID

    $ redis-cli> hget request:UUID request
    $ redis-cli> hget request:UUID response
    $ redis-cli> hget request:UUID aresponse

### Requests log

Every request's id is stored in sorted set (by timestamp).

  * key: **request-log**
    * score: **timestamp** - datetime converted to timestamp
    * member: **request:UUID** - where UUID is unique ID of request

To get log of last 20 requests:

    $ redis-cli> zrange request-log 0 20

# Async processing

Async processing starts when mode=ASYNC.
As immediate result APIGateway returns success code, if async processing started successfully and URL to ask for ultimate result.

Internally:
  * CallerService.invoke() method calls
    * private method that returns Observable (RxJava) with one result.
    * then
    * subscribes to observable in order to process response when external API finishes
    * then
    * returns simple Response with success and URL to ask for final response

# Commands to be used while developing

Test synchronous external service invocation:

    curl -X POST -H "Content-Type: application/json" -d@./src/test/groovy/online4m/apigateway/si/test/testdata.json http://localhost:5050/api/call

Test asynchronous external service invocation

    curl -X POST -H "Content-Type: application/json" -d@./src/test/groovy/online4m/apigateway/si/test/testdataasync.json http://localhost:5050/api/call

Load test. Change **-c** from 1 to more clients. Change **-r** from 1 to more repetition.

    siege -c 1 -r 1 -H 'Content-Type: application/json' 'http://localhost:5050/api/call POST < ./src/test/groovy/online4m/apigateway/si/test/testdata.json'

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
