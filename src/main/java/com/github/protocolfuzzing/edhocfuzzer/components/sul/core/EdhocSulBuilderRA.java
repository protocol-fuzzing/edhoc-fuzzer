package com.github.protocolfuzzing.edhocfuzzer.components.sul.core;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInputRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.AbstractSul;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SulBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;

public class EdhocSulBuilderRA implements SulBuilder<EdhocInputRA, EdhocOutputRA, EdhocExecutionContextRA>{
    @Override
    public AbstractSul<EdhocInputRA, EdhocOutputRA, EdhocExecutionContextRA>
    build(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        return new EdhocSulRA(sulConfig, cleanupTasks).initialize();
    }
}
