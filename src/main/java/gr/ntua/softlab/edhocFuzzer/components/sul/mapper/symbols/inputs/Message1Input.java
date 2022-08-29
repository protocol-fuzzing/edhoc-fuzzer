package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.inputs;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.initiator.Message1;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public class Message1Input extends EdhocInput {

    @Override
    public void preSendUpdate(ExecutionContext context) {
        if (getEdhocSessionPersistent(context).isInitiator()) {
            // Initiator by sending message 1 starts a new key exchange session
            // so previous session state must be cleaned
            getEdhocSessionPersistent(context).reset();
        }
    }

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new Message1(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<EdhocInputType> getInputType() {
        return EdhocInputType.EDHOC_MESSAGE_1;
    }
}
