package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator.EdhocMessage1;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;

public class EdhocMessage1Input extends EdhocInput {

    @Override
    public void preSendUpdate(EdhocExecutionContext context) {
        EdhocSessionPersistent session = context.getState().getEdhocSessionPersistent();

        if (session.isInitiator()) {
            // Initiator by sending message 1 starts a new key exchange session
            // so previous session state must be cleaned unless reset is disabled
            session.resetIfEnabled();
        }
    }

    @Override
    public EdhocProtocolMessage generateProtocolMessage(EdhocExecutionContext context) {
        return new EdhocMessage1(new MessageProcessorPersistent(context.getState()));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_MESSAGE_1;
    }
}
