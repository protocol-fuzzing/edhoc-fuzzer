package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.PayloadType;
import org.eclipse.californium.core.coap.CoAP;

public class ServerMapperConnector implements EdhocMapperConnector {
    // TODO fill


    @Override
    public void send(byte[] payload, PayloadType payloadType, CoAP.Code coapCode, int contentFormat) {

    }

    @Override
    public byte[] receive() {
        return new byte[0];
    }

    @Override
    public boolean isLatestResponseSuccessful() {
        return false;
    }

    @Override
    public boolean isLatestResponseSuccessfulAppData() {
        return false;
    }

    @Override
    public void setTimeout(Long timeout) {

    }
}
