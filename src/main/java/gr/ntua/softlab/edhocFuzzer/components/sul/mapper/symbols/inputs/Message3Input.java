package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.inputs;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.initiator.Message3;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public class Message3Input extends EdhocInput {

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new Message3(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<EdhocInputType> getInputType() {
        return EdhocInputType.EDHOC_MESSAGE_3;
    }
}
