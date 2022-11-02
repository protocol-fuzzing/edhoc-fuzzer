package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulServer;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.*;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.Bytes;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientMapperConnector implements EdhocMapperConnector {
    protected CoapClient edhocClient;
    protected CoapClient appClient;
    protected CoapEndpoint coapEndpoint;

    protected CoapResponse response;

    // Possible Codes: 0 [Generic Error], 1 [Unsupported Message]
    protected int exceptionCodeOccurred = -1;

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
        exceptionCodeOccurred = -1;
        currentCoapExchangeInfo = null;

        Request request = new Request(CoAP.Code.valueOf(messageCode), CoAP.Type.CON);
        request.getOptions().setContentFormat(contentFormat);
        request.setPayload(payload);

        try {
            switch (payloadType) {
                case EDHOC_MESSAGE -> {
                    response = edhocClient.advanced(request);
                }
                case UNPROTECTED_APP_MESSAGE -> {
                    response = appClient.advanced(request);
                }
                case PROTECTED_APP_MESSAGE -> {
                    request.getOptions().setOscore(new byte[0]);
                    response = appClient.advanced(request);
                }
                case MESSAGE_3_COMBINED -> {
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
    public boolean receivedError() {
        return response != null
                && response.advanced().isError();
    }

    @Override
    public boolean receivedProtectedAppMessage() {
        return isResponseSuccessful()
                && currentCoapExchangeInfo.hasProtectedMessage();
    }

    @Override
    public boolean receivedUnprotectedAppMessage() {
        return isResponseSuccessful()
                && currentCoapExchangeInfo.hasUnprotectedMessage()
                && !currentCoapExchangeInfo.hasEdhocMessage();
    }

    @Override
    public boolean receivedMsg3CombinedWithAppMessage() {
        return isResponseSuccessful()
                && currentCoapExchangeInfo.hasMsg3CombinedWithAppMessage();
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
