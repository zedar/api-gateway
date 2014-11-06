# Example: Salesforce.com Account API

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

There is one more attribute called **Security Token** that has to connected with consumer password.
If you do not know it, from top menu select *your name* and then **My profile**. From left side menu sslect **Reset My security Token**.
Click the button **Reset Security Token**. New token is delivered via email.

Detailed instructions could be found in [Force.com REST API Developer's Guide](https://www.salesforce.com/us/developer/docs/api_rest/).

### Start APIGateway

Start runtime dependencies (Redis key/value store should start)

    $ ./gradlew runREnv

Start APIGateway runtime

    @ ./gradlew run

## Get ACCESS TOKEN

Create new text file called: **sf_get_token.json** and put followin JSON into it:

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

As response in **data** attribute you should get *access_token* and the other important information:

    "data": {
      "access_token": "00D24000000H37Q!AQsAQIRpSF2fPMWPxIyINJD1i4DILFO4yHwAtuOjkPYBBYOV0JEDnB9L1rqzja.1BXHveUFqIMxXCOZDUByMuFnYu2dpPWrI",
      "id": "https://login.salesforce.com/id/00D24000000H37QEAS/00524000000Q57wAAC",
      "instance_url": "https://eu5.salesforce.com",
      "issued_at": "1415233444183",
      "signature": "wsDDfB6/HffugIfWjPQIs2X6sPHWj3e1QPKPX7VGats=",
      "token_type": "Bearer"
    }


