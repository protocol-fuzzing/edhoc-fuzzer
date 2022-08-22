package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.GenericErrorException;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.TimeoutException;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;

public class ServerMapperConnector implements EdhocMapperConnector {
    protected String host;
    protected int port;

    protected String edhocResource;
    protected String appGetResource;

    // timeout in milliseconds
    protected Long timeout;
    protected boolean exceptionOccurred;

    protected EdhocServer edhocServer = null;

    // shared wrapper that holds exchanges of requests, which await a response
    protected CoapExchangeWrapper coapExchangeWrapper;

    // current active CoAP exchange
    protected CoapExchange currentExchange;

    public ServerMapperConnector(String coapHost, String edhocResource, String appGetResource, Long originalTimeout) {
        this.edhocResource = edhocResource;
        this.appGetResource = appGetResource;
        this.timeout = originalTimeout;

        String[] hostAndPort = coapHost.replace("coap://", "").split(":");
        this.host = hostAndPort[0];
        this.port = Integer.parseInt(hostAndPort[1]);
    }

    public void initialize(EdhocStackFactoryPersistent edhocStackFactoryPersistent,
                           CoapExchangeWrapper coapExchangeWrapper) {
        this.coapExchangeWrapper = coapExchangeWrapper;

        // destroy last server
        if (edhocServer != null) {
            edhocServer.destroy();
        }

        // create new server
        edhocServer = new EdhocServer(host, port, edhocResource, appGetResource, edhocStackFactoryPersistent,
                coapExchangeWrapper);

        // start server
        edhocServer.start();
    }

    public void shutdown() {
        if (edhocServer != null) {
            edhocServer.destroy();
        }
    }

    public void waitForClientMessage() {
        currentExchange = coapExchangeWrapper.getCoapExchange();
        if (currentExchange != null) {
            return;
        }

        try {
            coapExchangeWrapper.wait();
            currentExchange = coapExchangeWrapper.getCoapExchange();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(byte[] payload, PayloadType payloadType, int messageCode, int contentFormat) {
        if (currentExchange == null) {
            return;
        }

        exceptionOccurred = false;

        Response response = new Response(CoAP.ResponseCode.valueOf(messageCode));
        response.getOptions().setContentFormat(contentFormat);

        switch (payloadType) {
            case EDHOC_MESSAGE ->
                    response.setPayload(payload);
            case APPLICATION_DATA -> {
                boolean oscoreProtectedExchange = currentExchange.advanced().getCryptographicContextID() != null;
                if (oscoreProtectedExchange) {
                    // request was oscore-protected so will be the response
                    // oscore protection is handled in edhoc layers
                    response.setPayload(payload);
                } else {
                    // request was not oscore-protected so won't be the response.
                    // Proceeding as the protected case would result in sending
                    // the payload unprotected, because the oscore layer would
                    // not have the information needed in order to protect the
                    // response. Unprotected empty coap ack is sent instead.
                    response.getOptions().setContentFormat(MediaTypeRegistry.UNDEFINED);
                    response.setPayload(new byte[0]);
                }
            }
            case MESSAGE_3_COMBINED -> {
                response.getOptions().setEdhoc(true);
                response.getOptions().setOscore(payload);
            }
        }

        coapExchangeWrapper.reset();
        currentExchange.respond(response);

        try {
            Thread.sleep(timeout);
            currentExchange = coapExchangeWrapper.getCoapExchange();
        } catch (InterruptedException e) {
            exceptionOccurred = true;
            currentExchange = null;
        }
    }

    @Override
    public byte[] receive() throws GenericErrorException, TimeoutException {
        if (currentExchange != null) {
            return currentExchange.advanced().getRequest().getPayload();
        }

        // exchange is null, something happened
        if (exceptionOccurred) {
            throw new GenericErrorException();
        } else {
            throw new TimeoutException();
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
        return coapExchangeWrapper.hasApplicationData();
    }

    @Override
    public boolean receivedAppDataCombinedWithMsg3() {
        return coapExchangeWrapper.hasApplicationDataAfterMessage3();
    }
}
