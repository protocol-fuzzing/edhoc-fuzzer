package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.initiator;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common.ApplicationMessage;

public class Message3Combined extends ApplicationMessage {

    public Message3Combined(MessageProcessorPersistent messageProcessorPersistent) {
        // initialize fields as ApplicationData
        super(messageProcessorPersistent);
        // oscore-protected application message combined with edhoc message 3
        payloadType = PayloadType.MESSAGE_3_COMBINED;
    }
}
