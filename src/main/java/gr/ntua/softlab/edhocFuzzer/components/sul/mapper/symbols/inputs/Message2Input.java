package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.inputs;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.responder.Message2;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs.EdhocOutputChecker;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs.EdhocOutputType;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutputChecker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public class Message2Input extends EdhocInput {

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return new Message2(new MessageProcessorPersistent(getEdhocMapperState(context)));
    }

    @Override
    public void postReceiveUpdate(AbstractOutput output, AbstractOutputChecker abstractOutputChecker,
                                  ExecutionContext context) {

        // In case of message3 as output setup new oscore context
        // In case of message3Combined as output, the oscore context will be setup in edhoc layer
        // in order for the upper layers to process the oscore-protected application request

        if (((EdhocOutputChecker) abstractOutputChecker).isMessage(output, EdhocOutputType.EDHOC_MESSAGE_3)) {
            getEdhocSessionPersistent(context).setupOscoreContext();
        }
    }

    @Override
    public Enum<EdhocInputType> getInputType() {
        return EdhocInputType.EDHOC_MESSAGE_2;
    }
}
