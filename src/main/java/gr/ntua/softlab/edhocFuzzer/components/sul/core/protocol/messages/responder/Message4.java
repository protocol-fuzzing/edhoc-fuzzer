package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.responder;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class Message4 extends EdhocProtocolMessage {

    public Message4(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage4();
    }
}
