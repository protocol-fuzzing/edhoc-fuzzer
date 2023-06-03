package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractOutput;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractOutputChecker;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.xml.AbstractInputXml;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;

public abstract class EdhocInput extends AbstractInputXml {
    public EdhocMapperState getEdhocMapperState(ExecutionContext context) {
        return (EdhocMapperState) context.getState();
    }

    public EdhocSessionPersistent getEdhocSessionPersistent(ExecutionContext context) {
        return getEdhocMapperState(context).getEdhocSessionPersistent();
    }

    @Override
    public void preSendUpdate(ExecutionContext context) {}

    @Override
    public void postSendUpdate(ExecutionContext context) {}

    @Override
    public void postReceiveUpdate(AbstractOutput output, AbstractOutputChecker abstractOutputChecker,
                                  ExecutionContext context) {}
}
