package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulServer;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.GenericErrorException;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.TimeoutException;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.CoapExchangeInfo;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.CoapExchanger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.exception.ConnectorException;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientMapperConnector implements EdhocMapperConnector {
    protected CoapClient edhocClient;
    protected CoapClient appClient;
    protected CoapEndpoint coapEndpoint;

    protected CoapResponse response;
    protected boolean exceptionOccurred;
    protected boolean latestAppRequest;

    protected CoapExchanger coapExchanger;
    protected CoapExchangeInfo currentCoapExchangeInfo;

    public ClientMapperConnector(String edhocUri, String appUri, Long originalTimeout){
        this.coapEndpoint = CoapEndpoint.builder().build();
        this.edhocClient = new CoapClient(edhocUri).setEndpoint(coapEndpoint).setTimeout(originalTimeout);
        this.appClient = new CoapClient(appUri).setEndpoint(coapEndpoint).setTimeout(originalTimeout);
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
        latestAppRequest = false;
        exceptionOccurred = false;
        currentCoapExchangeInfo = null;

        Request request = new Request(CoAP.Code.valueOf(messageCode), CoAP.Type.CON);
        request.getOptions().setContentFormat(contentFormat);

        try {
            switch (payloadType) {
                case EDHOC_MESSAGE -> {
                    request.setPayload(payload);
                    response = edhocClient.advanced(request);
                }
                case APPLICATION_DATA -> {
                    latestAppRequest = true;
                    request.getOptions().setOscore(payload);
                    response = appClient.advanced(request);
                }
                case MESSAGE_3_COMBINED -> {
                    latestAppRequest = true;
                    request.getOptions().setEdhoc(true);
                    request.getOptions().setOscore(payload);
                    response = appClient.advanced(request);
                }
            }
        } catch (ConnectorException | IOException e) {
            exceptionOccurred = true;
            response = null;
        } finally {
            // null on timeout or exception, but not null on successful exchange
            currentCoapExchangeInfo = coapExchanger.getReceivedQueue().poll();
        }
    }

    @Override
    public byte[] receive() throws GenericErrorException, TimeoutException {
        if (response != null) {
            return response.getPayload();
        }

        // response is null, something happened
        if (exceptionOccurred) {
            throw new GenericErrorException();
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public void setTimeout(Long timeout) {
        this.edhocClient.setTimeout(timeout);
        this.appClient.setTimeout(timeout);
    }

    @Override
    public boolean receivedError() {
        return response != null
                && response.advanced().isError();
    }

    @Override
    public boolean receivedAppData() {
        return isResponseSuccessful()
                && latestAppRequest
                && currentCoapExchangeInfo.hasApplicationData();
    }

    @Override
    public boolean receivedAppDataCombinedWithMsg3() {
        return receivedAppData()
                && currentCoapExchangeInfo.hasApplicationDataAfterMessage3();
    }

    public boolean receivedEmptyMessage() {
        return isResponseSuccessful()
                && response.getPayloadSize() == 0
                && response.advanced().getType() == CoAP.Type.ACK;
    }

    protected boolean isResponseSuccessful() {
        return response != null
                && response.advanced().isSuccess()
                && currentCoapExchangeInfo != null;
    }
}
