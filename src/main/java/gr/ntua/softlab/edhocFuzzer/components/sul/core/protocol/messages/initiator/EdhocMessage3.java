package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.initiator;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class EdhocMessage3 extends EdhocProtocolMessage {

    public EdhocMessage3(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage3();
    }
}
