package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core;

import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerEnabler;

public interface TestRunnerBuilder {
    TestRunner build(TestRunnerEnabler testRunnerEnabler);
}
