package io.github.protocolfuzzing.edhocfuzzer.components.sul.core;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.AbstractSUL;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SULBuilder;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SULWrapper;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SULWrapperStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SULConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;

public class EdhocSulBuilder implements SULBuilder<EdhocInput, EdhocOutput, EdhocExecutionContext>{
    @Override
    public AbstractSUL<EdhocInput, EdhocOutput, EdhocExecutionContext>
    buildSUL(SULConfig sulConfig, CleanupTasks cleanupTasks) {
        return new EdhocSul(sulConfig, cleanupTasks).initialize();
    }

    @Override
    public SULWrapper<EdhocInput, EdhocOutput, EdhocExecutionContext> buildWrapper() {
        return new SULWrapperStandard<>();
    }
}
