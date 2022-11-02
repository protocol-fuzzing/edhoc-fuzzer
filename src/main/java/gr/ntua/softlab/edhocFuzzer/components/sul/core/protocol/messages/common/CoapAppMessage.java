package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;

public class CoapAppMessage extends AppMessage {

    public CoapAppMessage(MessageProcessorPersistent messageProcessorPersistent) {
        // initialize fields as ApplicationData
        super(messageProcessorPersistent);
        // non oscore-protected application message
        payloadType = PayloadType.COAP_APP_MESSAGE;
    }
}
