package com.github.protocolfuzzing.edhocfuzzer.components.sul.core;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.AbstractSUL;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SULBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SULWrapper;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SULWrapperStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SULConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;

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
