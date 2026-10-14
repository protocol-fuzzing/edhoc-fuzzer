package io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.responder.EdhocMessage4;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;

public class EdhocMessage4Input extends EdhocInput {

    @Override
    public EdhocProtocolMessage generateProtocolMessage(EdhocExecutionContext context) {
        return new EdhocMessage4(new MessageProcessorPersistent(context.getState()));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_MESSAGE_4;
    }
}
