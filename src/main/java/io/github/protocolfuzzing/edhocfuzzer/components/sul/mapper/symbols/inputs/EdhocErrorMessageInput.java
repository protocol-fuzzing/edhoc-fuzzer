package io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.EdhocErrorMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;

public class EdhocErrorMessageInput extends EdhocInput {

    @Override
    public EdhocProtocolMessage generateProtocolMessage(EdhocExecutionContext context) {
        return new EdhocErrorMessage(new MessageProcessorPersistent(context.getState()));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_ERROR_MESSAGE;
    }
}
