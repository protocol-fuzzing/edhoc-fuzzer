package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.PayloadType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.exception.ConnectorException;

import java.io.IOException;
import java.net.InetSocketAddress;

/*
 * It is used when the Mapper should act as a client to connect
 * to a server SUL
 */
public class ClientMapperConnector implements EdhocMapperConnector {
    private static final Logger LOGGER = LogManager.getLogger();
    protected CoapClient edhocClient;
    protected CoapClient appClient;
    protected CoapEndpoint coapEndpoint;
    protected CoapResponse response;

    // Possible Codes: 0 [Generic Error], 1 [Unsupported Message]
    protected int exceptionCodeOccurred = -1;

    // indicate whether latest request was to application resource
    // so an application response is expected
    protected boolean expectedAppResponse = false;

    protected CoapExchanger coapExchanger;
    protected CoapExchangeInfo currentCoapExchangeInfo;

    protected Concretizer sendConcretizer = null;
    protected Concretizer recvConcretizer = null;

    public ClientMapperConnector(String edhocUri, String appUri, Long originalTimeout, String path) {
        this.coapEndpoint = CoapEndpoint.builder().build();
        this.edhocClient = new CoapClient(edhocUri).setEndpoint(coapEndpoint).setTimeout(originalTimeout);
        this.appClient = new CoapClient(appUri).setEndpoint(coapEndpoint).setTimeout(originalTimeout);
        if (path != null) {
            this.sendConcretizer = new Concretizer(path, "send");
            this.recvConcretizer = new Concretizer(path, "recv");
        }
    }

    @Override
    public void initialize(EdhocStackFactoryPersistent edhocStackFactoryPersistent,
                           CoapExchanger coapExchanger) {

        this.coapExchanger = coapExchanger;

        // create new coapEndpoint using provided stackFactory
        // at the same address as the previous one
        InetSocketAddress address = coapEndpoint.getAddress();
        coapEndpoint.destroy();
        coapEndpoint = CoapEndpoint.builder()
                .setInetSocketAddress(address)
                .setCoapStackFactory(edhocStackFactoryPersistent)
                .build();

        // set the new endpoint to clients
        edhocClient.setEndpoint(coapEndpoint);
        appClient.setEndpoint(coapEndpoint);
    }

    @Override
    public void send(byte[] payload, PayloadType payloadType, int messageCode, int contentFormat) {
        exceptionCodeOccurred = -1;
        expectedAppResponse = false;
        currentCoapExchangeInfo = null;

        if (payload == null) {
            LOGGER.error("Payload to send is null");
            exceptionCodeOccurred = 0;
            response = null;
            return;
        }

        Request request = new Request(CoAP.Code.valueOf(messageCode), CoAP.Type.CON);
        request.getOptions().setContentFormat(contentFormat);
        request.setPayload(payload);

        try {
            switch (payloadType) {
                case EDHOC_MESSAGE ->
                        response = edhocClient.advanced(request);
                case COAP_APP_MESSAGE -> {
                    expectedAppResponse = true;
                    response = appClient.advanced(request);
                }
                case OSCORE_APP_MESSAGE -> {
                    expectedAppResponse = true;
                    request.getOptions().setOscore(new byte[0]);
                    response = appClient.advanced(request);
                }
                case EDHOC_MESSAGE_3_OSCORE_APP -> {
                    expectedAppResponse = true;
                    request.getOptions().setEdhoc(true);
                    request.getOptions().setOscore(new byte[0]);
                    response = appClient.advanced(request);
                }
            }
        } catch (ConnectorException | IOException e) {
            exceptionCodeOccurred = 0;
            response = null;
        } finally {
            // null on timeout or exception, but not null on successful exchange
            currentCoapExchangeInfo = coapExchanger.getReceivedQueue().poll();
        }

        if (sendConcretizer != null) {
            sendConcretizer.concretize(request.getBytes());
        }
    }

    @Override
    public byte[] receive() throws GenericErrorException, TimeoutException, UnsupportedMessageException {
        // save code and reset it to maintain neutral state
        int code = exceptionCodeOccurred;
        exceptionCodeOccurred = -1;

        switch (code) {
            case 0 -> throw new GenericErrorException();
            case 1 -> throw new UnsupportedMessageException();
            default -> {
                if (response == null) {
                    throw new TimeoutException();
                }

                if (recvConcretizer != null) {
                    recvConcretizer.concretize(response.advanced().getBytes());
                }

                return response.getPayload();
            }
        }
    }

    @Override
    public void setTimeout(Long timeout) {
        this.edhocClient.setTimeout(timeout);
        this.appClient.setTimeout(timeout);
    }

    @Override
    public boolean receivedCoapErrorMessage() {
        return response != null
                && response.advanced().isError();
    }

    @Override
    public boolean receivedOscoreAppMessage() {
        // oscore app messages should be identified regardless of expectation
        boolean isReceived = isResponseSuccessful()
                && !currentCoapExchangeInfo.hasEdhocMessage()
                && currentCoapExchangeInfo.hasOscoreAppMessage();

        if (isReceived && !expectedAppResponse) {
            LOGGER.warn("Received OSCORE application message without prior request");
        }

        return isReceived;
    }

    @Override
    public boolean receivedCoapAppMessage() {
        // coap app message is identified only if it is expected
        // if it is not expected, the message is normally identified as coap message
        return isResponseSuccessful()
                && expectedAppResponse
                && !currentCoapExchangeInfo.hasEdhocMessage()
                && !currentCoapExchangeInfo.hasOscoreAppMessage();
    }

    @Override
    public boolean receivedMsg3WithOscoreApp() {
        return isResponseSuccessful()
                && currentCoapExchangeInfo.hasEdhocMessage()
                && currentCoapExchangeInfo.hasOscoreAppMessage();
    }

    @Override
    public boolean receivedCoapEmptyMessage() {
        return isResponseSuccessful()
                && response.getPayloadSize() == 0
                && response.advanced().getType() == CoAP.Type.ACK;
    }

    protected boolean isResponseSuccessful() {
        return response != null
                && response.advanced().isSuccess()
                && currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.getMID() == response.advanced().getMID();
    }

    public void shutdown() {
        if (sendConcretizer != null) {
            sendConcretizer.close();
        }
        if (recvConcretizer != null) {
            recvConcretizer.close();
        }
    }
}
