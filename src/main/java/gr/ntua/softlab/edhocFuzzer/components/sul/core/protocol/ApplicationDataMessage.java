package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import org.eclipse.californium.core.coap.CoAP;

public class ApplicationDataMessage extends EdhocProtocolMessage {
    public ApplicationDataMessage() {
        // empty payload for GET request
        payload = new byte[0];
        // GET request
        coapCode = CoAP.Code.GET;
        // -1 means null content format
        contentFormat = -1;
        // oscore-protected application message
        payloadType = PayloadType.APPLICATION_DATA;
    }
}
