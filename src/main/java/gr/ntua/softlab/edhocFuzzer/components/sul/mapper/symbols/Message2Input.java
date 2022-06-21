package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.Message2;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocState;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.xml.AbstractInputXml;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import org.eclipse.californium.edhoc.EdhocSession;

public class Message2Input extends AbstractInputXml {

    @Override
    public ProtocolMessage generateProtocolMessage(ExecutionContext context) {
        EdhocSession edhocSession = ((EdhocState) context.getState()).getEdhocSession();
        return new Message2(edhocSession, null);
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
    public Enum<EdhocInputType> getInputType() {
        return EdhocInputType.EDHOC_MESSAGE_2;
    }
}
