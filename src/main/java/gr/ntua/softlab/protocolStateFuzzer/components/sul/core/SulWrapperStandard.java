package gr.ntua.softlab.protocolStateFuzzer.components.sul.core;

import de.learnlib.api.SUL;
import de.learnlib.filter.statistic.Counter;
import de.learnlib.filter.statistic.sul.ResetCounterSUL;
import de.learnlib.filter.statistic.sul.SymbolCounterSUL;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers.*;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

public class SulWrapperStandard implements SulWrapper {
    private static final Logger LOGGER = LogManager.getLogger(SulWrapperStandard.class);
    protected SUL<AbstractInput, AbstractOutput> wrappedSul;
    protected Counter inputCounter;
    protected Counter testCounter;
    protected Duration timeLimit;
    protected Long testLimit;

    @Override
    public SulWrapper wrap(AbstractSul abstractSul) {
        wrappedSul = abstractSul;
        SulConfig sulConfig = abstractSul.getSulConfig();

        if (sulConfig.getCommand() != null) {
            wrappedSul = new AbstractProcessWrapper(wrappedSul, sulConfig);
        }

        if (sulConfig.getResetPort() != null) {
            if (sulConfig.isFuzzingClient()) {
                wrappedSul = new ResettingServerWrapper<>(wrappedSul, sulConfig, abstractSul.getCleanupTasks());
                abstractSul.setDynamicPortProvider((DynamicPortProvider) wrappedSul);
            }
            else {
                wrappedSul = new ResettingClientWrapper<>(wrappedSul, sulConfig, abstractSul.getCleanupTasks());
            }
        }

        wrappedSul = new AbstractIsAliveWrapper(wrappedSul, sulConfig.getMapperConfig());

//		if (!sulConfig.isFuzzingClient()) {
//			wrappedSul = new ClientConnectWrapper(wrappedSul);
//		}

        wrappedSul = new SymbolCounterSUL<>("symbol counter", wrappedSul);
        inputCounter = ((SymbolCounterSUL<AbstractInput, AbstractOutput>) wrappedSul).getStatisticalData();

        wrappedSul = new ResetCounterSUL<>("reset counter", wrappedSul);
        testCounter = ((ResetCounterSUL<AbstractInput, AbstractOutput>) wrappedSul).getStatisticalData();
        return this;
    }

    @Override
    public SulWrapper setTimeLimit(Duration timeLimit) {
        if (timeLimit == null || timeLimit.isNegative() || timeLimit.isZero()) {
            LOGGER.warn("Time limit given with erroneous value: " + timeLimit);
        } else if (this.timeLimit == null) {
            this.timeLimit = timeLimit;
            wrappedSul = new TimeoutWrapper<>(wrappedSul, timeLimit);
        } else {
            LOGGER.warn("Time limit for sul already set to " + timeLimit);
        }
        return this;
    }

    public SulWrapper setTestLimit(Long testLimit) {
        if (testLimit == null || testLimit <= 0L) {
            LOGGER.warn("Test limit with erroneous value: " + testLimit);
        } else if (this.testLimit == null) {
            this.testLimit = testLimit;
            wrappedSul = new TestLimitWrapper<>(wrappedSul, testLimit);
        } else {
            LOGGER.warn("Test limit for sul already set to " + testLimit);
        }
        return this;
    }

    public SUL<AbstractInput, AbstractOutput> getWrappedSul() {
        return wrappedSul;
    }

    @Override
    public Counter getInputCounter() {
        return inputCounter;
    }

    @Override
    public Counter getTestCounter() {
        return testCounter;
    }
}
