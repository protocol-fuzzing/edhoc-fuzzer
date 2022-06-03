package gr.ntua.softlab.protocolStateFuzzer.components.sul.core;

import de.learnlib.api.SUL;
import de.learnlib.filter.statistic.Counter;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;

import java.time.Duration;

public interface SulWrapper {
    SulWrapper wrap(AbstractSul abstractSul);

    SulWrapper setTimeLimit(Duration timeLimit);

    SulWrapper setTestLimit(Long testLimit);

    SUL<AbstractInput, AbstractOutput> getWrappedSul();

    Counter getInputCounter();

    Counter getTestCounter();
}
