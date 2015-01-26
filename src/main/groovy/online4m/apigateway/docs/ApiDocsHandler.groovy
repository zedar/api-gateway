package online4m.apigateway.docs

import javax.inject.Inject

import groovy.util.logging.Slf4j

import groovy.json.JsonOutput

import ratpack.groovy.handling.GroovyHandler
import ratpack.groovy.handling.GroovyContext

import online4m.apigateway.si.CallerServiceCtx

@Slf4j
class ApiDocsHandler extends GroovyHandler {
  @Inject CallerServiceCtx csCtx

  protected void handle(GroovyContext context) {
    def swgr = getSwaggerApi()
    log.debug("Swagger array type: ${swgr.getClass()}")
    context.with {
      byMethod {
        get {
          byContent {
            json {
              render JsonOutput.prettyPrint(JsonOutput.toJson(swgr))
            }
          }
        }
      }
    }
  }

  /**
   *  Definition of APIGateway's API in Swagger format.
   *  @return java.util.LinkedHashMap - swagger JSON
   */
  private def getSwaggerApi() {
    String serverUrl = csCtx.getHost() + (csCtx.getPort() ? ":${csCtx.getPort()}" : "")
    def swgr = [
      swagger:  "2.0",
      info: [
        title: "ApiGateway - unified access to REST/Web services",
        description: """
          Single entry point for invocation of diverse services.
          (*) all requests are handled asynchronously
          (*) unified input and output data format for gatway call
          (*) APIGateway is REST API itself
          (*) supported invocation modes: SYNC, ASYNC, EVENT (fire and forget)
          (*) APIGateway is ratpack.io service
        """,
        contact: [
          name: "zedar",
          url: "https://github.com/zedar"
        ],
        license: [
          name: "Creative Commons 4.0 International",
          url: "http://creativecommons.org/licenses/by/4.0/"
        ],
        version: "0.1.0"
      ],
      host: serverUrl,
      basePath: "/api",
      schemes: ["http", "https"],
      paths: [
        "/invoke": [
          post: [
            tags: ["api", "invoke", "route"],
            summary: "Invoke external API with unified request format",
            consumes: ["application/json"],
            produces: ["application/json"],
            parameters: [
              [
                name: "body",
                description: "API invoke request properties",
                in: "body",
                required: true,
                schema: [
                  "\$ref": "#/definitions/Request"
                ]
              ]
            ],
            responses: [
              "200": [
                description: "API Gateway successful response",
                schema: [
                  "\$ref": "#/definitions/Response"
                ]
              ],
              "default": [
                description: "Unexpected error",
                schema: [
                  "\$ref": "#/definitions/Error"
                ]
              ]
            ]
          ]
        ],
        "/invoke/{id}/request": [
          get: [
            tags: ["api", "invoke", "request"],
            summary: "Get request properties for the given unique request {id}",
            consumes: ["application/json"],
            produces: ["application/json"],
            parameters: [
              [
                name: "id",
                description: "UUID - unique API invocation ID",
                in: "path",
                required: true,
                type: "string"
              ]
            ],
            responses: [
              "200": [
                description: "API invocation request correlated with the given {id}",
                schema: [
                  "\$ref": "#/definitions/Request"
                ]
              ]
            ]
          ]
        ],
        "/invoke/{id}/response": [
          get: [
            tags: ["api", "invoke", "response"],
            summary: "Get response properties for the given unique request {id}",
            consumes: ["application/json"],
            produces: ["application/json"],
            parameters: [
              [
                name: "id",
                description: "UUID - unique API invocation ID",
                in: "path",
                required: true,
                type: "string"
              ]
            ],
            responses: [
              "200": [
                description: "API invocation response correlated with the given {id}",
                schema: [
                  "\$ref": "#/definitions/Response"
                ]
              ]
            ]
          ]
        ]
      ],
      definitions: [
        Request: [
          title: "Request",
          description: "API invoke request properties",
          type: "object",
          properties: [
            id: [
              type: "string",
              description: "UUID - Unique request ID"
            ],
            method: [
              enum: [
                "POST", "GET", "PUT", "PATCH", "DELETE"
              ],
              description: " API invocation method"
            ],
            mode: [
              enum: [
                "SYNC", "ASYNC", "EVENT"
              ],
              description: "SYNC - invoke API and wait for response, ASYNC - invoke API and request for its response, EVENT - invoke API and do not wait for response"
            ],
            format: [
              enum: [
                "JSON", "XML", "URLENC"
              ],
              description: "JSON: Content-Type: application/json, XML: Content-Type: application/xml, URLENC: Content-Type: application/x-www-form-urlencoded"
            ],
            url: [
              type: "string",
              description: "URL to invoke the API"
            ],
            headers: [
              type: "object",
              description: "Map (key:value) of headers to be set on API invocation"
            ],
            data: [
              type: "object",
              description: "HTTP request data to be sent as either query attributes or body content"
            ],
            links: [
              type: "object",
              description: "Map (key:value) of links to request, response from the given API invocation flow"
            ]
          ],
          required: [
            "method", "mode", "format", "url"
          ]
        ],
        Response: [
          title: "Response",
          description: "API invoke response properties",
          type: "object",
          properties: [
            success: [
              type: "boolean",
              description: "true - if API invocation finished without error, false otherwise"
            ],
            errorCode: [
              type: "string",
              description: "0 - if no error, else otherwise"
            ],
            errorDescr: [
              type: "string",
              description: "Error description"
            ],
            statusCode: [
              type: "integer",
              description: "External API invocation HTTP status code"
            ],
            id: [
              type: "string",
              description: "UUID - unique request ID"
            ],
            data: [
              type: "object",
              description: "JSON object with serialized output from external API invocation"
            ],
            href: [
              type: "string",
              description: "URL to get this response"
            ],
            links: [
              type: "object",
              description: "Map (key:value) of links to request, response from the given API invocation flow"
            ]
          ]
        ],
        Error: [
          title: "Error",
          description: "API invoke unexpected error properties",
          type: "object",
          properties: [
            errorCode: [
              type: "string",
              description: "0 - if no error, else otherwise"
            ],
            errorDescr: [
              type: "string",
              description: "Error description"
            ],
          ]
        ]
      ]
    ]
    return swgr
  }
}
