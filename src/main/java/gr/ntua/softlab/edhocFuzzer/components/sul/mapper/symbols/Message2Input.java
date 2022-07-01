package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.Message2;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public class Message2Input extends EdhocInput {

    @Override
    public void preSendUpdate(ExecutionContext context) {
    }

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        MessageProcessorPersistent messageProcessorPersistent = new MessageProcessorPersistent(
                getEdhocMapperState(context));
        return new Message2(messageProcessorPersistent);
    }

    @Override
    public void postSendUpdate(ExecutionContext context) {
    }

    @Override
    public void postReceiveUpdate(AbstractOutput output, ExecutionContext context) {
    }

    @Override
    public Enum<EdhocInputType> getInputType() {
        return EdhocInputType.EDHOC_MESSAGE_2;
    }
}
