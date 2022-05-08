package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearningConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulDelegate;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulServerDelegate;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeConfig;

@Parameters(commandDescription = "Performs state-fuzzing on a protocol client generating a model of the system")
public class StateFuzzerClientConfig extends StateFuzzerConfig {
	@ParametersDelegate
	protected SulServerDelegate sulServerDelegate;

	public StateFuzzerClientConfig(SulServerDelegate sulServerDelegate) {
		super();
		this.sulServerDelegate = sulServerDelegate;
	}

	public StateFuzzerClientConfig(LearningConfig learningConfig, MapperConfig mapperConfig,
								   TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig,
								   SulServerDelegate sulServerDelegate) {
		super(learningConfig, mapperConfig, testRunnerConfig, timingProbeConfig);
		this.sulServerDelegate = sulServerDelegate;
	}

	@Override
	public SulDelegate getSulDelegate() {
		return sulServerDelegate;
	}

	public boolean isClient() {
		return true;
	}

}
