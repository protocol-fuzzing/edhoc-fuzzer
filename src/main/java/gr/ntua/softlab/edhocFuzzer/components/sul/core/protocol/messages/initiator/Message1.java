package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.initiator;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class Message1 extends EdhocProtocolMessage {

    public Message1(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage1();
    }
}
