package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;

public class EdhocMessage3 extends EdhocProtocolMessage {

    public EdhocMessage3(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeMessage3();
    }
}
