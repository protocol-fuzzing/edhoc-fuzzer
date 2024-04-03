package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputBuilderRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputCheckerRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.MapperComposerRA;
import de.learnlib.ralib.words.PSymbolInstance;

public class EdhocMapperComposerRA extends
        MapperComposerRA<PSymbolInstance, EdhocProtocolMessage, EdhocExecutionContextRA, EdhocMapperState> {
    public EdhocMapperComposerRA(EdhocInputMapperRA edhocInputMapper, EdhocOutputMapperRA edhocOutputMapper) {
        super(edhocInputMapper, edhocOutputMapper);
    }

    @Override
    public EdhocOutputCheckerRA getOutputChecker() {
        return (EdhocOutputCheckerRA) super.getOutputChecker();
    }

    @Override
    public EdhocOutputBuilderRA getOutputBuilder() {
        return (EdhocOutputBuilderRA) super.getOutputBuilder();
    }
}
