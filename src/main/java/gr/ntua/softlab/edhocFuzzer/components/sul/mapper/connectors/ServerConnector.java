package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;

import java.io.IOException;

public class ServerConnector implements MapperConnector {
    protected CoapClient client;
    protected CoapResponse response;
    protected byte[] emptyPayload = new byte[0];

    public ServerConnector(CoapClient client){
        this.client = client;
    }

    @Override
    public void send(byte[] payload) {
        Request request = new Request(Code.POST, Type.CON);
        request.setPayload(payload);
        try {
            response = client.advanced(request);
        } catch (ConnectorException | IOException e) {
            response = null;
        }
    }

    @Override
    public byte[] receive() {
        return response == null ? emptyPayload : response.getPayload();
    }
}
