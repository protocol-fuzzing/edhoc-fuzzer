package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulServerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;

@Parameters(commandDescription = "Performs state-fuzzing on a protocol server generating a model of the system")
public class StateFuzzerServerConfig extends StateFuzzerConfig {
    @ParametersDelegate
    protected SulServerConfig sulServerConfig;

    public StateFuzzerServerConfig(SulServerConfig sulServerConfig) {
        super();
        this.sulServerConfig = sulServerConfig;
    }

    public StateFuzzerServerConfig(LearnerConfig learnerConfig, SulServerConfig sulServerConfig,
                                   TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig) {
        super(learnerConfig, testRunnerConfig, timingProbeConfig);
        this.sulServerConfig = sulServerConfig;
    }

    @Override
    public SulConfig getSulConfig() {
        return sulServerConfig;
    }

    public boolean isFuzzingClient() {
        return false;
    }
}
