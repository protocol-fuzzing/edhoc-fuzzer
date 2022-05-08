package gr.ntua.softlab.protocolStateFuzzer.testRunner;

import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerEnabler;

public interface TestRunnerBuilder {
    TestRunner build(TestRunnerEnabler testRunnerEnabler);
}
