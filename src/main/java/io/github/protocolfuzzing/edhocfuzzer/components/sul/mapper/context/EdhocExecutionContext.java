package io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContextStepped;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.StepContext;

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
