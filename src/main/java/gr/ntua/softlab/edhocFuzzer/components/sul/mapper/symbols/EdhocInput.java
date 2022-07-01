package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.xml.AbstractInputXml;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;

public abstract class EdhocInput extends AbstractInputXml {
    public EdhocMapperState getEdhocMapperState(ExecutionContext context) {
        return (EdhocMapperState) context.getState();
    }

    public EdhocSessionPersistent getEdhocSessionPersistent(ExecutionContext context) {
        EdhocMapperState edhocMapperState = getEdhocMapperState(context);
        return (EdhocSessionPersistent) edhocMapperState.getEdhocSession();
    }
}
