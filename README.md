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
It uses [*Gradle*](http://www.gradle.org) build subsystem and tends to follow [json:api](http://jsonapi.org/format/) standard.

API Gateway microservice is used by [online4m.com](https://www.online4m.com/online4m/info/howItWorks#howitworks) - pragmatic way to evolutionary develop workflow driven applications.

# API Gateway usage patterns

## Invoke external REST API synchronously

<img src="docs/images/APIGateway-SYNC.png" alt="Invoke external REST API synchronously" width="600">

Request in JSON format is a body of POST request to **api/invoke** endpoint. 
Response with external API output is a body of this endpoint result.

Request attributes:

  * mode=SYNC
  * method=GET|POST|PUT|DELETE
  * format=JSON|XML|URLENC


## Invoke external REST API asynchronously

<img src="docs/images/APIGateway-ASYNC.png" alt="Invoke external REST API asynchronously" width="600">

Request in JSON format is a body of POST request to **api/invoke** endpoint. 
Response is acknowledgment only. 
External API is called in separate thread and its output could be retrieved by **api/invoke/{id}/response** endpoint.

Request attributes:

  * mode=ASYNC
  * method=GET|POST|PUT|DELETE
  * format=JSON|XML|URLENC

# API specification

API specification tends to be inline with [json:api](http://jsonapi.org/format/) standard.

## HTTP headers

**GET**

    Accept: application/json | application/vnd.api+json

**POST**

    Content-Type: application/json | application/vnd.api+json
    Accept: application/json | application/vnd.api+json

## Endpoints

### /api

Get list of all available APIs

**Method:** GET  
**Accept:** application/json or application/vnd.api+json  
**HTTP return codes:**

  * 200 - OK


#### Output message format

    {
      "title": "Get list of all available APIs",
      "href": "http://localhost:5050/api",
      "links": {
          "invoke": {
            "href": "http://localhost:5050/api/invoke",
            "type": "api",
            "title": "POST request and invoke external API"
          },
          "request": {
            "href":   "http://localhost:5050/api/invoke/{id}/request",
            "type":   "api",
            "title":  "GET request, identified by {id}, that initialized external API call"
          },
          "response": {
            "href": "http://localhost:5050/api/invoke/{id}/response",
            "type": "api",
            "title": "GET response, identified by {id}, that is result of external API call"
          },
          "health-checks": {
            "href": "http://localhost:5050/api/health-checks",
            "type": "monitoring",
            "title": "Run all health checks"
          },
          "health-check-named": {
            "href": "http://localhost:5050/api/call/health-check/{name}",
            "type": "monitoring",
            "title": "Available: apigateway"
          }
      }
    }

### /api/invoke

Invoke external API either synchronously or asynchronously. 
Use diverse HTTP methods and formats for API invocations.

**Method:** POST  
**Content-Type:** application/json or application/vnd.api+json  
**Accept:** application/json or application/vnd.api+json  
**HTTP return codes:**

  * 200 - OK

#### Input message format

    {
      "request": {
        "id":       "Universal Unique Identifier (UUID)",
        "method":   "GET|POST|PUT|DELETE",
        "mode":     "SYNC|ASYNC|EVENT",
        "format":   "JSON|XML|URLENC",
        "url":      "URI OF EXTERNAL ENDPOINT",
        "headers":  JSON,
        "data":     JSON
      }
    }

where **request** attributes are:

**method:**

  * HTTP method to be used for external API call

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

  * JSON, either with list of query parameters or request body content.

#### Output message format

    {
      "response": {
        "success":        "true|false",
        "errorCode":      "0 if no error, else otherwise",
        "errorDescr":     "Error description",
        "data":           JSON WITH EXTERNAL API OUTPUT,
        "httpStatusCode": "HTTP status code from external API invoke",
        "id":             "Universal Unique Identifier (UUID)",
        "href":           "http://localhost:5050/api/invoke/{id}/response",
        "links": {
          "request": {
            "href":   "http://localhost:5050/api/invoke/{id}/request"
          }
        }
      }
    }

### /api/invoke/{id}/request

Get request that started invocation given by {id}.

**Method:** GET  
**Accept:** application/json or application/vnd.api+json  
**HTTP return codes:**

  * 200 - OK

#### Output message format

    {
      "request": {
        "id":       "Universal Unique Identifier (UUID)",
        "method":   "GET|POST|PUT|DELETE",
        "mode":     "SYNC|ASYNC|EVENT",
        "format":   "JSON|XML|URLENC",
        "url":      "URI OF EXTERNAL ENDPOINT",
        "headers":  JSON,
        "data":     JSON,
        "href":     "http://localhost:5050/api/invoke/{id}/request",
        "links": {
          "response": {
            "href": "http://localhost:5050/api/invoke/{id}/response"
          }
        }
      }
    }

### /api/invoke/{id}/response

Get response from external API invocation given by {id}.
If *mode*=SYNC, response with external API output data is returned inside */api/call* response.
If *mode*=ASYNC, response could be acknowledgment message (when async call has not been finished) or 
response from external API call (if async processing has finished).

**Method:** GET  
**Accept:** application/json or application/vnd.api+json  
**HTTP return codes:**

  * 200 - OK

#### Output message format

Response when only acknowlegment is available.

    {
      "response": {
        "success":        "true|false",
        "errorCode":      "0 if no error, else otherwise",
        "errorDescr":     "Error description",
        "httpStatusCode": "HTTP status code from external API invoke",
        "id":             "Universal Unique Identifier (UUID)",
        "href":           "http://localhost:5050/api/invoke/{id}/response",
        "links": {
          "request": {
            "href":   "http://localhost:5050/api/invoke/{id}/request"
          }
        }
      }
    }

Response when external API has finished and output is available.
It contains *data* attribute with JSON representation of external API output.

    {
      "response": {
        "success":        "true|false",
        "errorCode":      "0 if no error, else otherwise",
        "errorDescr":     "Error description",
        "data":           JSON WITH EXTERNAL API OUTPUT,
        "httpStatusCode": "HTTP status code from external API invoke",
        "id":             "Universal Unique Identifier (UUID)",
        "href":           "http://localhost:5050/api/invoke/{id}/response",
        "links": {
          "request": {
            "href":   "http://localhost:5050/api/invoke/{id}/request"
          }
        }
      }
    }

### api/health-checks

Run all health checks and return their values.

**Method:** GET

### api/health-check/:name

Run health check defined by the given :name.

**Method:** GET

Defined health checks:

  * *apigateway*

## Example API calls

### Get API endpoints

    $ curl -X GET -H "Accept: application/vnd.api+json" http://localhost:5050/api
or
    $ curl -X GET -H "Accept: application/json" http://localhost:5050/api

### Example: HipChat - get history of chats

Example HipChat API call:

    $ curl -X POST -H "Content-Type: application/json" -d '{"method": "GET", "mode": "SYNC", "format": "JSON", "url": "https://api.hipchat.com/v2/room/online4m.com/history/latest?auth_token=YOUR_TOKEN", "data": {"max-results": {"l": 10}}}' -i http://localhost:5050/api/call

### Example: Twitter query with OAUTH authorization

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
    initMounteBank  - initialize stubs configuration with *./src/test/resources/imposter.json* file.
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

    curl -X POST -H "Content-Type: application/json" -d@./src/test/resources/testdata.json http://localhost:5050/api/call

Test asynchronous external service invocation

    curl -X POST -H "Content-Type: application/json" -d@./src/test/resources/testdataasync.json http://localhost:5050/api/call

Load test. Change **-c** from 1 to more clients. Change **-r** from 1 to more repetition.

    siege -c 1 -r 1 -H 'Content-Type: application/json' 'http://localhost:5050/api/call POST < ./src/test/resources/testdata.json'

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
