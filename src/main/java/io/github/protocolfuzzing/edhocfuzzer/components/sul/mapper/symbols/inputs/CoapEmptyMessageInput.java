package io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.CoapEmptyMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;

public class CoapEmptyMessageInput extends EdhocInput {

    @Override
    public EdhocProtocolMessage generateProtocolMessage(EdhocExecutionContext context) {
        return new CoapEmptyMessage(new MessageProcessorPersistent(context.getState()));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.COAP_EMPTY_MESSAGE;
    }
}
