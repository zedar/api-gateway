# Example: Salesforce.com Account API

## Test case specification

Whole test for [Force.com](http://force.com) API is defined in 
[ForceDotComApiTest](https://github.com/zedar/api-gateway/tree/master/src/test/groovy/online4m/apigateway/func/ForceDotComApiTest.groovy).

Before running it please look at the [Prerequisites](#Prerequisites) section.
Especially you need developer account in Force.com.

Test case requires properties file. In folder *./src/test/resources* create file *force.com.properties* file with attributes:

    customer_key=Force.com Customer Key
    customer_secret=Force.com Customer Secret
    username=Force.com User Login (usually email)
    password=Force.com password followed by Security Token
    
Run just Force.com test by adding *-Dtest.single* attribute.

    $ ./gradlew -Dtest.single=*Force* test

## Prerequisites

### Salesforce.com developer registration

Register as developer in Salesforce.com: [https://developer.salesforce.com/signup](https://developer.salesforce.com/signup).

From top right menu button select option **Me Developer Account**. Next login is required.

On the left side find **Create** and then **Apps** menu. Under **Connected Apps** click **New** application. Fill out basic information:

  * **Connected App Name**: for example *online4m*
  * **API Name**: for example *online4m*
  * **Contact Email**: your email
  * Check **Enable OAuth Settings**
  * Enter **Callback URL**: for example https://localhost:8443/RestTest/oauth/callback. 
    * Callback is needed only for login with redirection to Salesforce.com.
    * In our case we are going to use app to app integration. No user interaction will be necessary.
  * From **Selected OAuth Scopes**: select *Full access* and click **Add** button.
  * **Save** new application

As result there are 2 very important values that you can find in application settings:

  * **Consumer Key**
  * **Consumer Secret**

We are going to use them in API to get *access token*.

There is one more attribute called **Security Token** that has to be connected with consumer password.
If you do not know it, from top menu select *your name* and then **My settings**. 
From left side menu sslect **Personal/Reset My security Token**.
Click the button **Reset Security Token**. New token is delivered via email.

Detailed instructions could be found in [Force.com REST API Developer's Guide](https://www.salesforce.com/us/developer/docs/api_rest/).

### Start APIGateway server and its dependencies

Start runtime dependencies (Redis key/value store should start)

    $ ./gradlew runREnv

Start APIGateway runtime

    @ ./gradlew run

## Get ACCESS TOKEN

Create new text file called: **sf_get_token.json** with APIGateway's JSON format:

    {
      "method": "POST",
      "mode": "SYNC",
      "format": "URLENC",
      "url": "https://login.salesforce.com/services/oauth2/token",
      "data": {
        "grant_type": "password",
        "client_id": PUT CONSUMER KEY HERE,
        "client_secret": PUT CONSUMER SECRET HERE,
        "username": PUT YOUR Salesforce.com developer username,
        "password": PUT YOUR Salesforce.com password SUCCEEDED BY SECURITY TOKEN
      }
    }

Invoke APIGateway:

    $ curl -X POST -d@./sf_get_token.json http://localhost:5050/api/invoke

The **data** attribute in response should contain *access_token* and the other important information:

    "data": {
      "access_token": "00D24000000H37Q!AQsAQHIGWI8S_tZV6rE4YejHhizmqfUyCGxrHTxuYqkZIjUhQMCTfbhATP.pIyjVk55GrtxsWK451AnP0I_KaiV27mGtxql1",
      "id": "https://login.salesforce.com/id/00D24000000H37QEAS/00524000000Q57wAAC",
      "instance_url": "https://eu5.salesforce.com",
      "issued_at": "1415233444183",
      "signature": "wsDDfB6/HffugIfWjPQIs2X6sPHWj3e1QPKPX7VGats=",
      "token_type": "Bearer"
    }

## Create new ACCOUNT

Create new text file called: **sf_new_account.json** with APIGateway's JSON format:

    {
      "method": "POST",
      "mode":   "SYNC",
      "format": "JSON",
      "url":    "https://eu5.salesforce.com/services/data/v32.0/sobjects/Account/",
      "headers": {
        "Authorization": "Bearer 00D24000000H37Q!AQsAQHIGWI8S_tZV6rE4YejHhizmqfUyCGxrHTxuYqkZIjUhQMCTfbhATP.pIyjVk55GrtxsWK451AnP0I_KaiV27mGtxql1"
      },
      "data": {
          "Name": "Test Company Name"
      }
    }

There are two important requirements:

  * *url* attribute has to point to *instance_url* returned in ACCESS TOKEN request;
  * *Authorization* header attribute has to contain auth method *Bearer* with *access_token* value returned in ACCESS TOKEN request

Invoke APIGateway:

  $ curl -X POST -d@./sf_new_account.json http://localhost:5050/api/invoke

The **data** attribute in response should look like this:

    "data": {
        "errors": [
        ],
        "id": "00124000001QeXWAA0",
        "success": true
    }

## Change ACCOUNT attributes

Salesforce uses HTTP PATCH method in order to update record's data.

Create new text file called: **sf_update_account.json** with APIGateway's JSON format:

    {
      "method": "PATCH",
      "mode":   "SYNC",
      "format": "JSON",
      "url":    "https://eu5.salesforce.com/services/data/v32.0/sobjects/Account/00124000001QeXWAA0",
      "headers": {
        "Authorization": "Bearer 00D24000000H37Q!AQsAQHIGWI8S_tZV6rE4YejHhizmqfUyCGxrHTxuYqkZIjUhQMCTfbhATP.pIyjVk55GrtxsWK451AnP0I_KaiV27mGtxql1"
      },
      "data": {
          "Name": "New name for Company 3",
          "BillingCity": "Warsaw"
      }
    }

There are two important requirements:

  * *method* attribute has to have value *PATCH*
  * *url* attribute has to point to *instance_url* returned in ACCESS TOKEN request and at the end contains *id* of previously created ACCOUNT;
  * *Authorization* header attribute has to contain auth method *Bearer* with *access_token* value returned in ACCESS TOKEN request

Salesforce does not return content after successful patching. HTTP 204 is returned.

Response data looks different in this case. The **data** attribute has null value and **statusCode** has 204 value.

    {
        "href": "http://localhost:5050/api/invoke/cc8ffbc1-f8d1-42ba-93eb-e24a1f3348dd/response",
        "errorDescr": "",
        "id": "cc8ffbc1-f8d1-42ba-93eb-e24a1f3348dd",
        "errorCode": "0",
        "links": {
            "request": {
                "href": "http://localhost:5050/api/invoke/cc8ffbc1-f8d1-42ba-93eb-e24a1f3348dd/request"
            }
        },
        "data": null,
        "success": true,
        "statusCode": 204
    }

## Get ACCOUNT attributes

Create new text file called **sf_get_account.json** with APIGateway's format:

    {
      "method": "GET",
      "mode":   "SYNC",
      "format": "JSON",
      "url":    "https://eu5.salesforce.com/services/data/v32.0/sobjects/Account/00124000001QeXWAA0",
      "headers": {
        "Authorization": "Bearer 00D24000000H37Q!AQsAQHIGWI8S_tZV6rE4YejHhizmqfUyCGxrHTxuYqkZIjUhQMCTfbhATP.pIyjVk55GrtxsWK451AnP0I_KaiV27mGtxql1"
      },
      "data": {
          "fields": "Name,AccountNumber,BillingCity"
      }
    }

There are two important requirements:

  * *url* attribute has to point to *instance_url* returned in ACCESS TOKEN request and at the end contains *id* of previously created ACCOUNT;
  * *Authorization* header attribute has to contain auth method *Bearer* with *access_token* value returned in ACCESS TOKEN request
  * *data/fields* attribute contains list of ACCOUNT attributes to be returned from Salesforce

## Delete ACCOUNT

Create new text file called **sf_delete_account.json** with APIGaeway's format:

    {
      "method": "DELETE",
      "mode":   "SYNC",
      "format": "JSON",
      "url":    "https://eu5.salesforce.com/services/data/v32.0/sobjects/Account/00124000001QeXWAA0",
      "headers": {
        "Authorization": "Bearer 00D24000000H37Q!AQsAQHIGWI8S_tZV6rE4YejHhizmqfUyCGxrHTxuYqkZIjUhQMCTfbhATP.pIyjVk55GrtxsWK451AnP0I_KaiV27mGtxql1"
      }
    }

There are two important requirements:

  * *url* attribute has to point to *instance_url* returned in ACCESS TOKEN request and at the end contains *id* of previously created ACCOUNT;
  * *Authorization* header attribute has to contain auth method *Bearer* with *access_token* value returned in ACCESS TOKEN request

Salesforce does not return content after successful delete. HTTP 204 is returned.

Response data looks as follows. The **data** attribute has null value while **statusCode** has 204 value.

    {
        "href": "http://localhost:5050/api/invoke/6ff72ae9-5793-4326-9659-b684f3d21085/response",
        "errorDescr": "",
        "id": "6ff72ae9-5793-4326-9659-b684f3d21085",
        "errorCode": "0",
        "links": {
            "request": {
                "href": "http://localhost:5050/api/invoke/6ff72ae9-5793-4326-9659-b684f3d21085/request"
            }
        },
        "data": null,
        "success": true,
        "statusCode": 204
    }


