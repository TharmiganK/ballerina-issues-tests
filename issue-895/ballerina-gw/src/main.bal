import ballerina/http;

http:Client swaggerPetStoreClient = new("https://petstore3.swagger.io", {
        secureSocket: {
            keyStore: {
                path: "/Users/tharmigan/Downloads/SSLBackendIssue/wso2am-micro-gw-macos-3.2.0.92-SNAPSHOT/runtime/bre/security/ballerinaKeystore.p12",
                password: "ballerina"
            },
            trustStore: {
                path: "/Users/tharmigan/Downloads/SSLBackendIssue/wso2am-micro-gw-macos-3.2.0.92-SNAPSHOT/runtime/bre/security/ballerinaTruststore.p12",
                password: "ballerina"
            }
        }
    }
);

@http:ServiceConfig {
    basePath: "/api/v3"
}
service gateway on new http:Listener(9090) {

    @http:ResourceConfig {
        methods:["GET"],
        path:"/pet/findByStatus"
    }
    resource function petFindByStatus (http:Caller outboundEp, http:Request req) {
        http:Response|error clientResponse = swaggerPetStoreClient->forward(<@untainted>req.rawPath, <@untainted>req);

        if(clientResponse is http:Response) {
            var outboundResult = outboundEp->respond(clientResponse);
        } else {
            http:Response res = new;
            res.statusCode = 500;
            string payload = "Error connecting to the back end";
            res.setPayload(payload);
            var outboundResult = outboundEp->respond(res);  
        }
    }
}