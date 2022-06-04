package gr.ntua.softlab.protocolStateFuzzer.components.sul.core;

import de.learnlib.api.SUL;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers.DynamicPortProvider;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;

public abstract class AbstractSul implements SUL<AbstractInput, AbstractOutput> {
    protected SulConfig sulConfig;
    protected CleanupTasks cleanupTasks;
    protected DynamicPortProvider dynamicPortProvider;
    protected Mapper mapper;

    public AbstractSul(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        this.sulConfig = sulConfig;
        this.cleanupTasks = cleanupTasks;
        // mapper will be provided in subclasses
        this.mapper = null;
    }

    public SulConfig getSulConfig() {
        return sulConfig;
    }

    public CleanupTasks getCleanupTasks() {
        return cleanupTasks;
    }

    public void setDynamicPortProvider(DynamicPortProvider dynamicPortProvider) {
        this.dynamicPortProvider = dynamicPortProvider;
    }

    public Mapper getMapper() {
        return mapper;
    }
}
