package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfigProvider;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfigProvider;

public interface StateFuzzerEnabler extends LearnerConfigProvider, SulConfigProvider {
    /**
     * @return true if analysis concerns a client implementation, false otherwise
     */
    boolean isFuzzingClient();

    /**
     * @return the output directory in which results should be saved
     */
    String getOutput();
}
