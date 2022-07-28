package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.edhoc.Constants;

public abstract class EdhocProtocolMessage extends ProtocolMessage {
    // payload of the message
    protected byte[] payload;

    // coap code used on message send
    protected CoAP.Code coapCode = CoAP.Code.POST;

    // message content format
    protected int contentFormat = Constants.APPLICATION_EDHOC_CBOR_SEQ;

    // type of payload
    protected PayloadType payloadType = PayloadType.EDHOC_MESSAGE;

    public byte[] getPayload() {
        return payload;
    }

    public CoAP.Code getCoapCode() {
        return coapCode;
    }

    public int getContentFormat() {
        return contentFormat;
    }

    public PayloadType getPayloadType() {
        return payloadType;
    }
}
