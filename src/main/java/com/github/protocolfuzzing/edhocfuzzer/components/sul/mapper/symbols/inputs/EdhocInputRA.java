package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.MapperInput;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public abstract class EdhocInputRA extends PSymbolInstance
        implements MapperInput<EdhocOutputRA, EdhocProtocolMessage, EdhocExecutionContextRA> {
    public abstract Enum<MessageInputType> getInputType();

    EdhocInputRA(ParameterizedSymbol baseSymbol, @SuppressWarnings("rawtypes") DataValue... parameterValues) {
        super(baseSymbol, parameterValues);
    }

    private long extendedWait = 0;

    @Override
    public void preSendUpdate(EdhocExecutionContextRA context) {
    }

    @Override
    public void postSendUpdate(EdhocExecutionContextRA context) {
    }

    @Override
    public void postReceiveUpdate(
            EdhocOutputRA output,
            OutputChecker<EdhocOutputRA> abstractOutputChecker,
            EdhocExecutionContextRA context) {
    }

    @Override
    public Long getExtendedWait() {
        return extendedWait;
    }

    @Override
    public void setExtendedWait(Long value) {
        extendedWait = value;
    }

    @Override
    public String getName() {
        return this.getBaseSymbol().getName();
    }
}
