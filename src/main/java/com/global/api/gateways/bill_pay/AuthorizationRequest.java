package com.global.api.gateways.bill_pay;

import com.global.api.builders.AuthorizationBuilder;
import com.global.api.entities.Transaction;
import com.global.api.entities.billing.Credentials;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.GatewayException;
import com.global.api.entities.exceptions.UnsupportedTransactionException;
import com.global.api.gateways.bill_pay.requests.GetAchTokenRequest;
import com.global.api.gateways.bill_pay.requests.GetTokenRequest;
import com.global.api.gateways.bill_pay.requests.MakeBlindPaymentRequest;
import com.global.api.gateways.bill_pay.requests.MakeBlindPaymentReturnTokenRequest;
import com.global.api.gateways.bill_pay.requests.MakePaymentRequest;
import com.global.api.gateways.bill_pay.requests.MakePaymentReturnTokenRequest;
import com.global.api.gateways.bill_pay.responses.TokenRequestResponse;
import com.global.api.gateways.bill_pay.responses.TransactionResponse;
import com.global.api.paymentMethods.eCheck;
import com.global.api.utils.Element;
import com.global.api.utils.ElementTree;

public class AuthorizationRequest extends GatewayRequestBase {
    private static final String GENERIC_PAYMENT_EXCEPTION_MESSAGE = "An error occurred attempting to make the payment";

    public AuthorizationRequest(Credentials credentials, String serviceUrl, int timeout) {
        this.credentials = credentials;
        this.serviceUrl = serviceUrl;
        this.timeout = timeout;
    }

    public Transaction execute(AuthorizationBuilder builder, boolean isBillDataHosted)
            throws ApiException {
        switch (builder.getTransactionType()) {
            case Sale:
                if (isBillDataHosted) {
                    if (builder.isRequestMultiUseToken()) {
                        return makePaymentReturnToken(builder);
                    }

                    return makePayment(builder);
                }

                if (builder.isRequestMultiUseToken()) {
                    return makeBlindPaymentReturnToken(builder);
                }

                return makeBlindPayment(builder);
            case Verify:
                if (!builder.isRequestMultiUseToken()) {
                    throw new UnsupportedTransactionException();
                }

                if (builder.getPaymentMethod() instanceof eCheck) {
                    return getAchToken(builder);
                }

                return getToken(builder);
            default:
                throw new UnsupportedTransactionException();
        }
    }

    private Transaction makePaymentReturnToken(AuthorizationBuilder builder) throws ApiException {
        ElementTree et = new ElementTree();
        Element envelope = createSOAPEnvelope(et, "MakePaymentReturnToken");
        String request = new MakePaymentReturnTokenRequest(et)
            .build(envelope, builder, credentials);

        String response = doTransaction(request, publicEndpoint);
        Transaction result = new TransactionResponse()
            .withResponseTagName("MakePaymentReturnTokenResponse")
            .withResponse(response)
            .map();

        if (result.getResponseCode().equals("0")) {
            return result;
        }

        throw new GatewayException(GENERIC_PAYMENT_EXCEPTION_MESSAGE, result.getResponseCode(), result.getResponseMessage());
    }

    private Transaction makeBlindPaymentReturnToken(AuthorizationBuilder builder) throws ApiException {
        ElementTree et = new ElementTree();
        Element envelope = createSOAPEnvelope(et, "MakeBlindPaymentReturnToken");
        String request = new MakeBlindPaymentReturnTokenRequest(et)
            .build(envelope, builder, credentials);
        String response = doTransaction(request, publicEndpoint);
        Transaction result = new TransactionResponse()
            .withResponseTagName("MakeBlindPaymentReturnTokenResponse")
            .withResponse(response)
            .map();

        if (result.getResponseCode().equals("0")) {
            return result;
        }

        throw new GatewayException(GENERIC_PAYMENT_EXCEPTION_MESSAGE, result.getResponseCode(), result.getResponseMessage());
    }

    private Transaction makeBlindPayment(AuthorizationBuilder builder) throws ApiException {
        ElementTree et = new ElementTree();
        Element envelope = createSOAPEnvelope(et, "MakeBlindPayment");
        String request = new MakeBlindPaymentRequest(et)
            .build(envelope, builder, credentials);
        String response = doTransaction(request, publicEndpoint);
        Transaction result = new TransactionResponse()
            .withResponseTagName("MakeBlindPaymentResponse")
            .withResponse(response)
            .map();

        if (result.getResponseCode().equals("0")) {
            return result;
        }

        throw new GatewayException(GENERIC_PAYMENT_EXCEPTION_MESSAGE, result.getResponseCode(), result.getResponseMessage());
    }

    private Transaction makePayment(AuthorizationBuilder builder) throws ApiException {
        ElementTree et = new ElementTree();
        Element envelope = createSOAPEnvelope(et, "MakePayment");
        String request = new MakePaymentRequest(et)
            .build(envelope, builder, credentials);
        String response = doTransaction(request, publicEndpoint);
        Transaction result = new TransactionResponse()
            .withResponseTagName("MakePaymentResponse")
            .withResponse(response)
            .map();

        if (result.getResponseCode().equals("0")) {
            return result;
        }

        throw new GatewayException(GENERIC_PAYMENT_EXCEPTION_MESSAGE, result.getResponseCode(), result.getResponseMessage());
    }

    private Transaction getToken(AuthorizationBuilder builder) throws ApiException {
        ElementTree et = new ElementTree();
        Element envelope = createSOAPEnvelope(et, "GetToken");
        String request = new GetTokenRequest(et)
            .build(envelope, builder, credentials);
        String response = doTransaction(request, publicEndpoint);
        Transaction result = new TokenRequestResponse()
            .withResponseTagName("GetTokenResponse")
            .withResponse(response)
            .map();

        if (result.getResponseCode().equals("0")) {
            return result;
        }

        throw new GatewayException("An error occurred attempting to create the token", result.getResponseCode(), result.getResponseMessage());
    }

    private Transaction getAchToken(AuthorizationBuilder builder) throws ApiException {
        ElementTree et = new ElementTree();
        Element envelope = createSOAPEnvelope(et, "GetToken");
        String request = new GetAchTokenRequest(et)
            .build(envelope, builder, credentials);
        String response = doTransaction(request, publicEndpoint);
        Transaction result = new TokenRequestResponse()
            .withResponseTagName("GetTokenResponse")
            .withResponse(response)
            .map();

        if (result.getResponseCode().equals("0")) {
            return result;
        }

        throw new GatewayException("An error occurred attempting to create the token", result.getResponseCode(), result.getResponseMessage());
    }
}
