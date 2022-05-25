package gr.ntua.softlab.protocolStateFuzzer.testRunner.config;

import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearnerConfigProvider;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfigProvider;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulConfigProvider;

public interface TestRunnerEnabler extends TestRunnerConfigProvider, LearnerConfigProvider,
        MapperConfigProvider, SulConfigProvider {
}
