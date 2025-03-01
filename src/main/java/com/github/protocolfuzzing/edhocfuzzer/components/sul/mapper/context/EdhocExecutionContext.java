package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContextStepped;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.StepContext;

public class EdhocExecutionContext
extends ExecutionContextStepped<EdhocInput, EdhocOutput, EdhocMapperState, StepContext<EdhocInput, EdhocOutput>> {

    public EdhocExecutionContext(EdhocMapperState state) {
        super(state);
    }

    @Override
    protected StepContext<EdhocInput, EdhocOutput> buildStepContext() {
        return new StepContext<>(stepContexts.size());
    }
}
