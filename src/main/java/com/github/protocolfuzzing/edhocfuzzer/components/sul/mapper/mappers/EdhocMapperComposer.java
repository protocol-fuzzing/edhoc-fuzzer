package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputChecker;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.MapperComposer;

public class EdhocMapperComposer extends MapperComposer<EdhocInput, EdhocOutput, EdhocProtocolMessage, EdhocExecutionContext, EdhocMapperState> {
    public EdhocMapperComposer(EdhocInputMapper edhocInputMapper, EdhocOutputMapper edhocOutputMapper) {
        super(edhocInputMapper, edhocOutputMapper);
    }

    @Override
    public EdhocOutputChecker getOutputChecker() {
        return (EdhocOutputChecker) super.getOutputChecker();
    }

    @Override
    public OutputBuilder<EdhocOutput> getOutputBuilder() {
        return super.getOutputBuilder();
    }
}
