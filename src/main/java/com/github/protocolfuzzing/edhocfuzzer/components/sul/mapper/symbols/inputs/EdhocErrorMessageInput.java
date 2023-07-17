package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.protocol.ProtocolMessage;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.common.EdhocErrorMessage;

public class EdhocErrorMessageInput extends EdhocInput {

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new EdhocErrorMessage(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_ERROR_MESSAGE;
    }
}
