package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.initiator;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class Message3 extends EdhocProtocolMessage {

    public Message3(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage3();
    }
}
