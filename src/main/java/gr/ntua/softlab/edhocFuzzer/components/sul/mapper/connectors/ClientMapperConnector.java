package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.edhoc.Constants;
import org.eclipse.californium.elements.exception.ConnectorException;

import java.io.IOException;

public class ClientMapperConnector implements EdhocMapperConnector {
    protected CoapClient client;
    protected CoapResponse response;
    protected boolean timedOut;

    public ClientMapperConnector(String uri, Long originalTimeout){
        this.client = new CoapClient(uri);
        this.client.setTimeout(originalTimeout);
    }

    @Override
    public void send(byte[] payload) {
        Request request = new Request(Code.POST, Type.CON);
        request.getOptions().setContentFormat(Constants.APPLICATION_CID_EDHOC_CBOR_SEQ);
        request.setPayload(payload);
        timedOut = false;
        try {
            response = client.advanced(request);
        } catch (ConnectorException | IOException e) {
            if (request.isSent() && request.isTimedOut()) {
                timedOut = true;
            }
            response = null;
        }
    }

    @Override
    public byte[] receive() {
        if (response == null) {
            // indicate timeout by returning empty payload
            // and other error by returning null
            return timedOut ? new byte[0] : null;
        } else {
            // payload, if correct, won't be empty
            return response.getPayload();
        }
    }

    @Override
    public void setTimeout(Long timeout) {
        this.client.setTimeout(timeout);
    }
}
