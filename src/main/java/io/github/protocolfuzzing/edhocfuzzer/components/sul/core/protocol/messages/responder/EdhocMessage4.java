package io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.responder;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class EdhocMessage4 extends EdhocProtocolMessage {

    public EdhocMessage4(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage4();
    }
}
