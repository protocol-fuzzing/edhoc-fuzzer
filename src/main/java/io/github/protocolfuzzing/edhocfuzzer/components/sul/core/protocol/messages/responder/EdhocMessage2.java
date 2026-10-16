package io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.responder;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class EdhocMessage2 extends EdhocProtocolMessage {

    public EdhocMessage2(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage2();
    }
}
