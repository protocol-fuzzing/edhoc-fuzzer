package gr.ntua.softlab.protocolStateFuzzer.sul;

import de.learnlib.api.SUL;
import de.learnlib.filter.statistic.Counter;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulDelegate;
import gr.ntua.softlab.protocolStateFuzzer.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;

import java.time.Duration;

public interface WrappedSulBuilder {
    SUL<AbstractInput, AbstractOutput> build(SulDelegate sulDelegate, Mapper mapper, CleanupTasks cleanupTasks);

    void setTimeLimit(SUL<AbstractInput, AbstractOutput>  sul, Duration timeLimit);

    Counter getInputCounter();

    Counter getResetCounter();
}
