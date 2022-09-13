package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.*;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.util.concurrent.TimeUnit;

public class ServerMapperConnector implements EdhocMapperConnector {
    protected String host;
    protected int port;

    protected String edhocResource;
    protected String appGetResource;

    // timeout in milliseconds
    protected Long timeout;
    //protected boolean exceptionOccurred;
    // Possible Codes: 0 [Generic Error], 1 [Unsupported Message]
    protected int exceptionCodeOccurred = -1;

    protected EdhocServer edhocServer = null;

    protected CoapExchanger coapExchanger;
    protected CoapExchangeInfo currentCoapExchangeInfo;

    public ServerMapperConnector(String coapHost, String edhocResource, String appGetResource, Long originalTimeout) {
        this.edhocResource = edhocResource;
        this.appGetResource = appGetResource;
        this.timeout = originalTimeout;

        String[] hostAndPort = coapHost.replace("coap://", "").split(":");
        this.host = hostAndPort[0];
        this.port = Integer.parseInt(hostAndPort[1]);
    }

    public void initialize(EdhocStackFactoryPersistent edhocStackFactoryPersistent,
                           CoapExchanger coapExchanger) {
        this.coapExchanger = coapExchanger;

        // destroy last server
        if (edhocServer != null) {
            edhocServer.destroy();
        }

        // create new server
        edhocServer = new EdhocServer(host, port, edhocResource, appGetResource, edhocStackFactoryPersistent,
                coapExchanger);

        // start server
        edhocServer.start();
    }

    public void shutdown() {
        if (edhocServer != null) {
            edhocServer.destroy();
        }
    }

    public void waitForClientMessage() {
        try {
            // blocks until an element is available
            currentCoapExchangeInfo = coapExchanger.getReceivedQueue().take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(byte[] payload, PayloadType payloadType, int messageCode, int contentFormat) {
        //exceptionOccurred = false;
        exceptionCodeOccurred = -1;

        if (currentCoapExchangeInfo == null || currentCoapExchangeInfo.getCoapExchange() == null) {
            return;
        }

        CoapExchange currentExchange = currentCoapExchangeInfo.getCoapExchange();

        Response response = new Response(CoAP.ResponseCode.valueOf(messageCode));
        response.getOptions().setContentFormat(contentFormat);

        switch (payloadType) {
            case EDHOC_MESSAGE ->
                    response.setPayload(payload);
            case APPLICATION_DATA -> {
                if (currentExchange.advanced().getCryptographicContextID() != null) {
                    // request was oscore-protected so will be the response
                    // oscore protection is handled in edhoc layers
                    response.setPayload(payload);
                } else {
                    // request was not oscore-protected so no oscore-protected app message
                    // can be sent back, so it is deemed unsupported message
                    // Current exchange message is NOT consumed, this allows learning to
                    // continue and transition 'app / unsupported' to be regarded as self-loop
                    exceptionCodeOccurred = 1;
                    return;
                }
            }
            case MESSAGE_3_COMBINED -> {
                // ServerMapperConnector cannot use message 3 combined as a response
                // It can be used only as a request from a CoAP client as Initiator
                // So this message is deemed unsupported
                // Current exchange message is NOT consumed, this allows learning to
                // continue and transition 'msg3 / unsupported' to be regarded as self-loop
                exceptionCodeOccurred = 1;
                return;
            }
        }

        currentExchange.respond(response);

        try {
            currentCoapExchangeInfo = coapExchanger.getReceivedQueue().poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //exceptionOccurred = true;
            exceptionCodeOccurred = 0;
            currentCoapExchangeInfo = null;
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
                if (currentCoapExchangeInfo == null || currentCoapExchangeInfo.getCoapExchange() == null) {
                    throw new TimeoutException();
                } else {
                    return currentCoapExchangeInfo.getCoapExchange().advanced().getRequest().getPayload();
                }
            }
        }
    }

    @Override
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean receivedError() {
        // response in the server's case is a possible new request from the client,
        // therefore error response is neither an empty new exchange nor a non-empty one
        // there is no way to tell if client's new request (or the absence of it) is
        // an error
        return false;
    }

    @Override
    public boolean receivedAppData() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.hasApplicationData();
    }

    @Override
    public boolean receivedAppDataCombinedWithMsg3() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.hasApplicationDataAfterMessage3();
    }

    @Override
    public boolean receivedEmptyMessage() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.getCoapExchange() != null
                && currentCoapExchangeInfo.getCoapExchange().advanced().getRequest().getPayloadSize() == 0;
    }
}