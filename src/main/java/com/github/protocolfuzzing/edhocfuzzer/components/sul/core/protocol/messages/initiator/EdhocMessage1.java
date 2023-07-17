package gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.initiator;

import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class EdhocMessage1 extends EdhocProtocolMessage {

    public EdhocMessage1(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage1();
    }
}
