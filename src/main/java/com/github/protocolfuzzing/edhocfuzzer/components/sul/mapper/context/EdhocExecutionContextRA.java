package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContextStepped;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.StepContext;
import de.learnlib.ralib.words.PSymbolInstance;

public class EdhocExecutionContextRA extends
        ExecutionContextStepped<PSymbolInstance, PSymbolInstance, EdhocMapperState, StepContext<PSymbolInstance, PSymbolInstance>> {

    public EdhocExecutionContextRA(EdhocMapperState state) {
        super(state);
    }

    @Override
    protected StepContext<PSymbolInstance, PSymbolInstance> buildStepContext() {
        return new StepContext<>(stepContexts.size());
    }
}
