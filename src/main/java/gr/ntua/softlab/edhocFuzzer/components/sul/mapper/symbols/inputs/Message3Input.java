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
    public void postSendUpdate(ExecutionContext context) {
        // after generating message 3 and storing necessary information
        // to session, oscore context can be derived
        getEdhocSessionPersistent(context).setupOscoreContext();
    }

    @Override
    public Enum<EdhocInputType> getInputType() {
        return EdhocInputType.EDHOC_MESSAGE_3;
    }
}
