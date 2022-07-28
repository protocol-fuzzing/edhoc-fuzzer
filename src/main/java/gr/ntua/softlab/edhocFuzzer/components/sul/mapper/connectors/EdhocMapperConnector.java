package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.PayloadType;
import org.eclipse.californium.core.coap.CoAP;

public interface EdhocMapperConnector {
    void send(byte[] payload, PayloadType payloadType, CoAP.Code coapCode, int contentFormat);
    byte[] receive();
    boolean isLatestResponseSuccessful();

    boolean isLatestResponseSuccessfulAppData();

    void setTimeout(Long timeout);
}
