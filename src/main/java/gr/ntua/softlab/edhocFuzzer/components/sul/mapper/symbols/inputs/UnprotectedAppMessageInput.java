package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.inputs;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common.UnprotectedApplicationMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public class UnprotectedAppMessageInput extends EdhocInput {

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new UnprotectedApplicationMessage(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.UNPROTECTED_APP_MESSAGE;
    }
}
