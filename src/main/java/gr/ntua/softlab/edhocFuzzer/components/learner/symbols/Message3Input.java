package gr.ntua.softlab.edhocFuzzer.components.learner.symbols;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.xml.AbstractInputXml;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;

public class Message3Input extends AbstractInputXml {

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        return null;
    }

    @Override
    public void preSendUpdate(ExecutionContext context) {

    }

    @Override
    public void postSendUpdate(ExecutionContext context) {

    }

    @Override
    public void postReceiveUpdate(AbstractOutput output, ExecutionContext context) {

    }

    @Override
    public Enum<?> getInputType() {
        return null;
    }
}
