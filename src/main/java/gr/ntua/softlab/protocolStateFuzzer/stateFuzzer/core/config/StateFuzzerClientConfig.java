package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulClientConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;

@Parameters(commandDescription = "Performs state-fuzzing on a protocol client generating a model of the system")
public class StateFuzzerClientConfig extends StateFuzzerConfig {
	@ParametersDelegate
	protected SulClientConfig sulClientConfig;

	public StateFuzzerClientConfig(SulClientConfig sulClientConfig) {
		super();
		this.sulClientConfig = sulClientConfig;
	}

	public StateFuzzerClientConfig(LearnerConfig learnerConfig, MapperConfig mapperConfig,
                                   TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig,
                                   SulClientConfig sulClientConfig) {
		super(learnerConfig, mapperConfig, testRunnerConfig, timingProbeConfig);
		this.sulClientConfig = sulClientConfig;
	}

	@Override
	public SulConfig getSulConfig() {
		return sulClientConfig;
	}

	public boolean isFuzzingClient() {
		return true;
	}

}
