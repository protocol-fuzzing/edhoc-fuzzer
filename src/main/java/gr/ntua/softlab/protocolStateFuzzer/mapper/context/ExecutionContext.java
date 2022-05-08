package gr.ntua.softlab.protocolStateFuzzer.mapper.context;

import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;

public interface ExecutionContext {
    State getState();

    void disableExecution();

    void enableExecution();

    boolean isExecutionEnabled();

    void setInput(AbstractInput input);
}
