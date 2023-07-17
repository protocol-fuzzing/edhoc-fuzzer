package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.PayloadType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.util.concurrent.TimeUnit;

/*
 * It is used when the Mapper should act as a server to connect
 * to a client SUL
 */
public class ServerMapperConnector implements EdhocMapperConnector {
    private static final Logger LOGGER = LogManager.getLogger();
    protected String host;
    protected int port;
    protected String edhocResource;
    protected String appResource;

    // timeout in milliseconds
    protected Long timeout;

    // Possible Codes: 0 [Generic Error], 1 [Unsupported Message]
    protected int exceptionCodeOccurred = -1;

    protected EdhocServer edhocServer = null;
    protected CoapExchanger coapExchanger;
    protected CoapExchangeInfo currentCoapExchangeInfo;

    public ServerMapperConnector(String coapHost, String edhocResource, String appResource, Long originalTimeout) {
        this.edhocResource = edhocResource;
        this.appResource = appResource;
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
        edhocServer = new EdhocServer(host, port, edhocResource, appResource, edhocStackFactoryPersistent,
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
        exceptionCodeOccurred = -1;

        if (currentCoapExchangeInfo == null || currentCoapExchangeInfo.getCoapExchange() == null) {
            // timeout has occurred on previous send, so poll the queue to get a
            // possible message that has arrived after the previous send
            // although the learner will not be aware of it
            currentCoapExchangeInfo = coapExchanger.getReceivedQueue().poll();
            if (currentCoapExchangeInfo == null || currentCoapExchangeInfo.getCoapExchange() == null) {
                // impossible to send given message so is deemed unsupported
                LOGGER.warn("Unable to reply with given message: No active CoAP exchange found");
                exceptionCodeOccurred = 1;
                return;
            }
        }

        if (payload == null) {
            LOGGER.error("Payload to send is null");
            exceptionCodeOccurred = 0;
            currentCoapExchangeInfo = null;
            return;
        }

        CoapExchange currentExchange = currentCoapExchangeInfo.getCoapExchange();

        Response response = new Response(CoAP.ResponseCode.valueOf(messageCode));
        response.getOptions().setContentFormat(contentFormat);
        response.setPayload(payload);

        switch (payloadType) {
            case EDHOC_MESSAGE, COAP_APP_MESSAGE -> {
                if (currentExchange.advanced().getCryptographicContextID() != null) {
                    // request was oscore-protected but the response should be not
                    // so the oscore flag and the generated cryptographic context
                    // should be removed from the exchange

                    currentExchange.getRequestOptions().removeOscore();
                    currentExchange.advanced().setCryptographicContextID(null);
                }
            }
            case OSCORE_APP_MESSAGE -> {
                if (currentExchange.advanced().getCryptographicContextID() == null) {
                    // request was not oscore-protected so no oscore-protected app message
                    // can be sent back, so it is deemed unsupported message
                    // Current exchange message is NOT consumed, this allows learning to
                    // continue and transition 'app_msg / unsupported' to be regarded as self-loop
                    exceptionCodeOccurred = 1;
                    return;
                }
            }
            case EDHOC_MESSAGE_3_OSCORE_APP -> {
                // Cannot use message 3 combined with oscore app as a response
                // It can be used only as a request from a CoAP client as Initiator
                // So this message is deemed unsupported
                // Current exchange message is NOT consumed, this allows learning to
                // continue and transition 'msg3_app / unsupported' to be regarded as self-loop
                exceptionCodeOccurred = 1;
                return;
            }
        }

        currentExchange.respond(response);

        try {
            currentCoapExchangeInfo = coapExchanger.getReceivedQueue().poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            exceptionCodeOccurred = 0;
            currentCoapExchangeInfo = null;
        }
    }

    @Override
    public byte[] receive() throws GenericErrorException, TimeoutException, UnsupportedMessageException, UnsuccessfulMessageException {
        // save code and reset it to maintain neutral state
        int code = exceptionCodeOccurred;
        exceptionCodeOccurred = -1;

        switch (code) {
            case 0 -> throw new GenericErrorException();
            case 1 -> throw new UnsupportedMessageException();
            default -> {
                if (currentCoapExchangeInfo == null || currentCoapExchangeInfo.getCoapExchange() == null) {
                    throw new TimeoutException();
                }

                if (currentCoapExchangeInfo.hasUnsuccessfulMessage()) {
                    throw new UnsuccessfulMessageException();
                }

                return currentCoapExchangeInfo.getCoapExchange().advanced().getRequest().getPayload();
            }
        }
    }

    @Override
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean receivedCoapErrorMessage() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.getCoapExchange().advanced().getResponse() != null
                && currentCoapExchangeInfo.getCoapExchange().advanced().getResponse().isError();
    }

    @Override
    public boolean receivedOscoreAppMessage() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.hasOscoreAppMessage();
    }


    @Override
    public boolean receivedCoapAppMessage() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.hasCoapAppMessage();
    }

    @Override
    public boolean receivedMsg3WithOscoreApp() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.hasEdhocMessage()
                && currentCoapExchangeInfo.hasOscoreAppMessage();
    }

    @Override
    public boolean receivedCoapEmptyMessage() {
        return currentCoapExchangeInfo != null
                && currentCoapExchangeInfo.getCoapExchange() != null
                && currentCoapExchangeInfo.getCoapExchange().advanced().getRequest() != null
                && currentCoapExchangeInfo.getCoapExchange().advanced().getRequest().getPayloadSize() == 0;
    }
}
