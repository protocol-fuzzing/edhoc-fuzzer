package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import org.eclipse.californium.core.coap.CoAP;

public interface EdhocMapperConnector {
    void send(byte[] payload, PayloadType payloadType, CoAP.Code coapCode, int contentFormat);

    byte[] receive() throws GenericErrorException, TimeoutException;

    void setTimeout(Long timeout);

    boolean isLatestResponseSuccessful();

    boolean isLatestResponseSuccessfulAppData();

    boolean isLatestResponseEmptyCoapAck();
}
