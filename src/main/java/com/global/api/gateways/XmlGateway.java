package com.global.api.gateways;

import com.global.api.entities.exceptions.GatewayException;

public abstract class XmlGateway extends Gateway {
    public XmlGateway() {
        super("text/xml");
    }

    public String doTransaction(String request) throws GatewayException {
        return doTransaction(request, "");
    }
    public String doTransaction(String request, String endpoint) throws GatewayException {
        GatewayResponse response = sendRequest("POST", endpoint, request);
        if(response.getStatusCode() != 200)
            throw new GatewayException("Unexpected http status code [" + response.getStatusCode() + "]");
        return response.getRawResponse();
    }
}
