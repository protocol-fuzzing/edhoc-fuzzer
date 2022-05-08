package gr.ntua.softlab.protocolStateFuzzer.testRunner.config;

import gr.ntua.softlab.protocolStateFuzzer.learner.config.AlphabetOptionProvider;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfigProvider;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulDelegateProvider;

public interface TestRunnerEnabler extends TestRunnerConfigProvider, AlphabetOptionProvider, MapperConfigProvider,
        SulDelegateProvider {
}
