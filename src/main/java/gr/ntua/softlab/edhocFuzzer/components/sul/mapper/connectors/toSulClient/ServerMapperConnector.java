package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.GenericErrorException;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.TimeoutException;
import org.eclipse.californium.core.coap.CoAP;
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

    public ServerMapperConnector(String coapHost, String edhocResource, String appGetResource, Long originalTimeout) {
        this.edhocResource = edhocResource;
        this.appGetResource = appGetResource;
        this.timeout = originalTimeout;

        String[] hostAndPort = coapHost.replace("coap://", "").split(":");
        this.host = hostAndPort[0];
        this.port = Integer.parseInt(hostAndPort[1]);

        this.coapExchangeWrapper = new CoapExchangeWrapper();
    }

    @Override
    public void initialize(EdhocStackFactoryPersistent edhocStackFactoryPersistent) {
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

    public void waitForClientMessage() {
        if (coapExchangeWrapper.getCoapExchange() != null) {
            return;
        }

        try {
            coapExchangeWrapper.wait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(byte[] payload, PayloadType payloadType, int messageCode, int contentFormat) {
        CoapExchange currentExchange = coapExchangeWrapper.getCoapExchange();
        if (currentExchange == null) {
            return;
        }

        exceptionOccurred = false;

        Response response = new Response(CoAP.ResponseCode.valueOf(messageCode));
        response.getOptions().setContentFormat(contentFormat);
        response.setPayload(payload);

        coapExchangeWrapper.setCoapExchange(null);
        currentExchange.respond(response);

        if (timeout != null) {
            try {
                coapExchangeWrapper.wait(timeout);
            } catch (InterruptedException e) {
                exceptionOccurred = true;
                coapExchangeWrapper.setCoapExchange(null);
            }
        }
    }

    @Override
    public byte[] receive() throws GenericErrorException, TimeoutException {
        if (coapExchangeWrapper.getCoapExchange() != null) {
            return coapExchangeWrapper.getCoapExchange().advanced().getRequest().getPayload();
        }

        // exchange is null, something happened
        if (exceptionOccurred) {
            throw new GenericErrorException();
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public boolean isLatestResponseSuccessful() {
        // every new exchange is a new request from client
        // that corresponds to the client's response
        // successful is a response that invoked a new exchange
        return coapExchangeWrapper.getCoapExchange() != null;
    }


    @Override
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }
}
