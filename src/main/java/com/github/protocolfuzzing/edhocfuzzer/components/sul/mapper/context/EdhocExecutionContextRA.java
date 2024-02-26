package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInputRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContextStepped;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.StepContext;

public class EdhocExecutionContextRA
extends ExecutionContextStepped<EdhocInputRA, EdhocOutputRA, EdhocMapperState, StepContext<EdhocInputRA, EdhocOutputRA>> {

    public EdhocExecutionContextRA(EdhocMapperState state) {
        super(state);
    }

    @Override
    protected StepContext<EdhocInputRA, EdhocOutputRA> buildStepContext() {
        return new StepContext<>(stepContexts.size());
    }
}
