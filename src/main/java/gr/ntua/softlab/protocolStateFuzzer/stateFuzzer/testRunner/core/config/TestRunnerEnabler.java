package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config;

import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfigProvider;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfigProvider;

public interface TestRunnerEnabler extends TestRunnerConfigProvider, LearnerConfigProvider, SulConfigProvider {
}
