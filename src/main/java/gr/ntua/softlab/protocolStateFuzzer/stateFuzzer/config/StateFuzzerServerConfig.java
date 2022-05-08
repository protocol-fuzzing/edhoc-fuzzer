package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearningConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulClientDelegate;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulDelegate;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeConfig;

@Parameters(commandDescription = "Performs state-fuzzing on a protocol server generating a model of the system")
public class StateFuzzerServerConfig extends StateFuzzerConfig {
	public StateFuzzerServerConfig(SulClientDelegate sulClientDelegate) {
		super();
		this.sulClientDelegate = sulClientDelegate;
	}

	public StateFuzzerServerConfig(LearningConfig learningConfig, MapperConfig mapperConfig,
								   TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig,
								   SulClientDelegate sulClientDelegate) {
		super(learningConfig, mapperConfig, testRunnerConfig, timingProbeConfig);
		this.sulClientDelegate = sulClientDelegate;
	}

	@ParametersDelegate
	protected SulClientDelegate sulClientDelegate;

	@Override
	public SulDelegate getSulDelegate() {
		return sulClientDelegate;
	}

	public boolean isClient() {
		return false;
	}
}
