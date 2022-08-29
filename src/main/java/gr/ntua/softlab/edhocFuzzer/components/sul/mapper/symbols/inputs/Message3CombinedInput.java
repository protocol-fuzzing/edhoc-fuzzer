package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.inputs;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.initiator.Message3Combined;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public class Message3CombinedInput extends ApplicationDataInput {

    @Override
    public void preSendUpdate(ExecutionContext context) {
        // construct Message3 in order to store it in session 'message3' field,
        // derive new oscore context and make Message3 available to oscore layer
        new MessageProcessorPersistent(getEdhocMapperState(context)).writeMessage3();
    }

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new Message3Combined(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<EdhocInputType> getInputType() {
        return EdhocInputType.EDHOC_MESSAGE_3_COMBINED;
    }
}
