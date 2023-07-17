package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.responder;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class EdhocMessage2 extends EdhocProtocolMessage {

    public EdhocMessage2(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage2();
    }
}
