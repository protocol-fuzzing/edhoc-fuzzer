package gr.ntua.softlab.protocolStateFuzzer.mapper.mappers;

import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.sul.protocol.ProtocolMessage;

public abstract class InputMapper {
    public void sendInput(AbstractInput input, ExecutionContext context) {
        ProtocolMessage message = input.generateProtocolMessage(context);
		input.preSendUpdate(context);
		sendMessage(message, context);
		input.postSendUpdate(context);
    }

    protected abstract void sendMessage(ProtocolMessage message, ExecutionContext context);

    public void postReceive(AbstractInput input, AbstractOutput output, ExecutionContext context) {
        input.postReceiveUpdate(output, context);
    }
}
