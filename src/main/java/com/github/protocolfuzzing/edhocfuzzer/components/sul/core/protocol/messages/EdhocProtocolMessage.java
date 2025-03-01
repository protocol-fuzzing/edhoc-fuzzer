package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.edhoc.Constants;

public abstract class EdhocProtocolMessage {
    // payload of the message
    protected byte[] payload;

    // coap code used on message send
    protected int messageCode;

    // message content format
    protected int contentFormat;

    // type of payload
    protected PayloadType payloadType;

    public EdhocProtocolMessage(MessageProcessorPersistent messageProcessorPersistent) {
        payloadType = PayloadType.EDHOC_MESSAGE;

        if (messageProcessorPersistent.getEdhocMapperState().isCoapClient()) {
            // mapper is CoAP client and Initiator or Responder

            // message is Coap request with Coap Code POST
            messageCode = CoAP.Code.POST.value;

            // C_R or 'true' is prepended as Initiator
            // C_I is prepended as Responder
            contentFormat = Constants.APPLICATION_CID_EDHOC_CBOR_SEQ;
        } else {
            // mapper is CoAP server and Initiator or Responder

            // message is Coap response with Response Code Changed
            messageCode = CoAP.ResponseCode.CHANGED.value;

            // normal content format without prepended connection identifiers
            contentFormat = Constants.APPLICATION_EDHOC_CBOR_SEQ;
        }
    }

    public byte[] getPayload() {
        return payload;
    }

    public int getMessageCode() {
        return messageCode;
    }

    public int getContentFormat(boolean oldVersion) {
        if (oldVersion) {
            return switch (contentFormat) {
                case Constants.APPLICATION_EDHOC_CBOR_SEQ -> 65000;
                case Constants.APPLICATION_CID_EDHOC_CBOR_SEQ -> 65001;
                default -> contentFormat;
            };
        }

        return contentFormat;
    }

    public PayloadType getPayloadType() {
        return payloadType;
    }
}
