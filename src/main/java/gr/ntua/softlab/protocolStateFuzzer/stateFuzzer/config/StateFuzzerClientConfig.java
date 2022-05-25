package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulServerConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;

@Parameters(commandDescription = "Performs state-fuzzing on a protocol client generating a model of the system")
public class StateFuzzerClientConfig extends StateFuzzerConfig {
	@ParametersDelegate
	protected SulServerConfig sulServerConfig;

	public StateFuzzerClientConfig(SulServerConfig sulServerConfig) {
		super();
		this.sulServerConfig = sulServerConfig;
	}

	public StateFuzzerClientConfig(LearnerConfig learnerConfig, MapperConfig mapperConfig,
                                   TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig,
                                   SulServerConfig sulServerConfig) {
		super(learnerConfig, mapperConfig, testRunnerConfig, timingProbeConfig);
		this.sulServerConfig = sulServerConfig;
	}

	@Override
	public SulConfig getSulConfig() {
		return sulServerConfig;
	}

	public boolean isClient() {
		return true;
	}

}
