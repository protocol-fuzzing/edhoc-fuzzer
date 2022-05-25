package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearnerConfigProvider;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfigProvider;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulConfigProvider;

public interface StateFuzzerEnabler extends LearnerConfigProvider, MapperConfigProvider, SulConfigProvider {
    /**
     * @return true if analysis concerns a client implementation, false otherwise
     */
    boolean isClient();

    /**
     * @return the output directory in which results should be saved
     */
    String getOutput();
}
