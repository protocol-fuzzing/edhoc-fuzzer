package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.inputs;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.responder.EdhocMessage4;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public class EdhocMessage4Input extends EdhocInput {

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new EdhocMessage4(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_MESSAGE_4;
    }
}
