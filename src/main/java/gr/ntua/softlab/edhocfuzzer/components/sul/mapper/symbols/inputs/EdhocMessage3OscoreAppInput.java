package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.protocol.ProtocolMessage;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.initiator.EdhocMessage3OscoreApp;

public class EdhocMessage3OscoreAppInput extends EdhocInput {

    @Override
    public void preSendUpdate(ExecutionContext context) {
        // construct Message3 in order to store it in session 'message3' field,
        // derive new oscore context and make Message3 available to oscore layer
        new MessageProcessorPersistent(getEdhocMapperState(context)).writeMessage3();
    }

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new EdhocMessage3OscoreApp(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_MESSAGE_3_OSCORE_APP;
    }
}
