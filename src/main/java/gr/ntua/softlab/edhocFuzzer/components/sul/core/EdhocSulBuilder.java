package gr.ntua.softlab.edhocFuzzer.components.sul.core;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.AbstractSul;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.SulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;

public class EdhocSulBuilder implements SulBuilder {
    @Override
    public AbstractSul build(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        return new EdhocSul(sulConfig, cleanupTasks);
    }
}
