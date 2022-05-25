package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulClientConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;

@Parameters(commandDescription = "Performs state-fuzzing on a protocol server generating a model of the system")
public class StateFuzzerServerConfig extends StateFuzzerConfig {
	@ParametersDelegate
	protected SulClientConfig sulClientConfig;

	public StateFuzzerServerConfig(SulClientConfig sulClientConfig) {
		super();
		this.sulClientConfig = sulClientConfig;
	}

	public StateFuzzerServerConfig(LearnerConfig learnerConfig, MapperConfig mapperConfig,
                                   TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig,
                                   SulClientConfig sulClientConfig) {
		super(learnerConfig, mapperConfig, testRunnerConfig, timingProbeConfig);
		this.sulClientConfig = sulClientConfig;
	}

	@Override
	public SulConfig getSulConfig() {
		return sulClientConfig;
	}

	public boolean isClient() {
		return false;
	}
}
