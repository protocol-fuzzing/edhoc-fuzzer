package gr.ntua.softlab.protocolStateFuzzer.components.sul.core;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;

public interface SulBuilder {
    AbstractSul build(SulConfig sulConfig, CleanupTasks cleanupTasks);
}
