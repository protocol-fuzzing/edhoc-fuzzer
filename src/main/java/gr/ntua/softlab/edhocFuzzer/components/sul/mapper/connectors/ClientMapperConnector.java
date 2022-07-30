package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.exception.ConnectorException;

import java.io.IOException;

public class ClientMapperConnector implements EdhocMapperConnector {
    protected String edhocUri;
    protected CoapClient edhocClient = null;
    protected String appUri;
    protected CoapClient appClient = null;
    protected Long originalTimeout;
    protected CoapResponse response;
    protected boolean exceptionOccurred;
    protected boolean latestAppRequest;

    public ClientMapperConnector(String edhocUri, String appUri, Long originalTimeout){
        this.edhocUri = edhocUri;
        this.appUri = appUri;
        this.originalTimeout = originalTimeout;
    }

    public void createNewClients(EdhocStackFactoryPersistent edhocStackFactoryPersistent) {
        // shutdown old existing clients
        if (edhocClient != null) {
            edhocClient.shutdown();
        }

        if (appClient != null) {
            appClient.shutdown();
        }

        // new endpoint with new edhoc stack
        CoapEndpoint coapEndpoint = CoapEndpoint.builder().setCoapStackFactory(edhocStackFactoryPersistent).build();
        this.edhocClient = new CoapClient(edhocUri).setEndpoint(coapEndpoint).setTimeout(originalTimeout);
        this.appClient = new CoapClient(appUri).setEndpoint(coapEndpoint).setTimeout(originalTimeout);
    }

    @Override
    public void send(byte[] payload, PayloadType payloadType, Code coapCode, int contentFormat) {
        Request request = new Request(coapCode, Type.CON);
        request.getOptions().setContentFormat(contentFormat);
        latestAppRequest = false;
        exceptionOccurred = false;

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
            response = null;
            exceptionOccurred = true;
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
    public boolean isLatestResponseSuccessful() {
        return response != null
                && response.isSuccess();
    }

    @Override
    public boolean isLatestResponseSuccessfulAppData() {
        return isLatestResponseSuccessful()
                && latestAppRequest;
    }

    @Override
    public boolean isLatestResponseEmptyCoapAck() {
        return isLatestResponseSuccessful()
                && response.getPayloadSize() == 0
                && response.advanced().getType() == Type.ACK;
    }
}
