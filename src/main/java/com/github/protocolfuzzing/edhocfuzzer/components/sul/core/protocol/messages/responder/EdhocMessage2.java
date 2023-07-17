package gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.responder;

import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class EdhocMessage2 extends EdhocProtocolMessage {

    public EdhocMessage2(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage2();
    }
}
