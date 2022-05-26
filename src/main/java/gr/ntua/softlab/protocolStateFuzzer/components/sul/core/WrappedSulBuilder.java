package gr.ntua.softlab.protocolStateFuzzer.components.sul.core;

import de.learnlib.api.SUL;
import de.learnlib.filter.statistic.Counter;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;

import java.time.Duration;

public interface WrappedSulBuilder {
    SUL<AbstractInput, AbstractOutput> build(SulConfig sulConfig, MapperBuilder mapperBuilder, CleanupTasks cleanupTasks);

    void setTimeLimit(SUL<AbstractInput, AbstractOutput>  sul, Duration timeLimit);

    Counter getInputCounter();

    Counter getResetCounter();
}
