package io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.OscoreAppMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;

public class OscoreAppMessageInput extends EdhocInput {

    @Override
    public EdhocProtocolMessage generateProtocolMessage(EdhocExecutionContext context) {
        return new OscoreAppMessage(new MessageProcessorPersistent(context.getState()));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.OSCORE_APP_MESSAGE;
    }
}
