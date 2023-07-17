package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator.EdhocMessage1;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.protocol.ProtocolMessage;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContext;

public class EdhocMessage1Input extends EdhocInput {

    @Override
    public void preSendUpdate(ExecutionContext context) {
        if (getEdhocSessionPersistent(context).isInitiator()) {
            // Initiator by sending message 1 starts a new key exchange session
            // so previous session state must be cleaned unless reset is disabled
            getEdhocSessionPersistent(context).resetIfEnabled();
        }
    }

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new EdhocMessage1(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_MESSAGE_1;
    }
}
