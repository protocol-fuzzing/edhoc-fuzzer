package gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;

public interface ExecutionContext {
    State getState();

    void disableExecution();

    void enableExecution();

    boolean isExecutionEnabled();

    void setInput(AbstractInput input);
}
