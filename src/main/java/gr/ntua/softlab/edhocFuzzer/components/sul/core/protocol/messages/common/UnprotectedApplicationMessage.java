package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;

public class UnprotectedApplicationMessage extends ApplicationMessage {

    public UnprotectedApplicationMessage(MessageProcessorPersistent messageProcessorPersistent) {
        // initialize fields as ApplicationData
        super(messageProcessorPersistent);
        // non oscore-protected application message
        payloadType = PayloadType.UNPROTECTED_APP_MESSAGE;
    }
}
