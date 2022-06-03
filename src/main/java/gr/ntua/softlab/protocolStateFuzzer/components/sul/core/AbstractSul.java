package gr.ntua.softlab.protocolStateFuzzer.components.sul.core;

import de.learnlib.api.SUL;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers.DynamicPortProvider;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;

public abstract class AbstractSul implements SUL<AbstractInput, AbstractOutput> {
    protected SulConfig sulConfig;
    protected Mapper mapper;
    protected CleanupTasks cleanupTasks;
    protected DynamicPortProvider dynamicPortProvider;

    public AbstractSul(SulConfig sulConfig, MapperBuilder mapperBuilder, CleanupTasks cleanupTasks) {
        this.sulConfig = sulConfig;
        this.mapper = mapperBuilder.build(sulConfig.getMapperConfig(), sulConfig.isFuzzingClient());
        this.cleanupTasks = cleanupTasks;
    }

    public SulConfig getSulConfig() {
        return sulConfig;
    }

    public Mapper getMapper() {
        return mapper;
    }

    public CleanupTasks getCleanupTasks() {
        return cleanupTasks;
    }

    public void setDynamicPortProvider(DynamicPortProvider dynamicPortProvider) {
        this.dynamicPortProvider = dynamicPortProvider;
    }
}
