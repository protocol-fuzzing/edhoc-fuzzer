package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractInputXml;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.MapperInput;

import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;

public abstract class EdhocInputRA extends PSymbolInstance
        implements MapperInput<EdhocOutputRA, EdhocProtocolMessage, EdhocExecutionContextRA> {
    public abstract Enum<MessageInputType> getInputType();

    EdhocInputRA(ParameterizedSymbol baseSymbol, DataValue... parameterValues) {
        super(baseSymbol, parameterValues);
    }

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
}
