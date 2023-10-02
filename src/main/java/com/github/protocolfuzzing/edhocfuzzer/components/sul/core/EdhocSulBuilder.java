package com.github.protocolfuzzing.edhocfuzzer.components.sul.core;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.AbstractSul;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SulBuilder;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;

public class EdhocSulBuilder implements SulBuilder {
    private boolean concretize;

    public EdhocSulBuilder(boolean concretize) {
        this.concretize = concretize;
    }

    @Override
    public AbstractSul build(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        return new EdhocSul(sulConfig, cleanupTasks, concretize);
    }
}
