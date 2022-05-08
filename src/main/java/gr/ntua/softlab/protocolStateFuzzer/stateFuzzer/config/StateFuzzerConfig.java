package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.AlphabetOptionProvider;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearningConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulDelegateProvider;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeEnabler;

public abstract class StateFuzzerConfig extends ToolConfig implements TimingProbeEnabler, TestRunnerEnabler,
		AlphabetOptionProvider, RoleProvider, SulDelegateProvider {
	
	@Parameter(names = "-alphabet", required = false, description = "A file defining the input alphabet. "
			+ "The alphabet is used to interpret inputs from a given specification, as well as to learn. "
			+ "Each input in the alphabet has a name under which it appears in the specification."
			+ "The name can be changed by setting the 'name' attribute in xml format for example.")
	protected String alphabet = null;

	@Parameter(names = "-output", required = false, description = "The folder in which results should be saved")
	protected String output = "output";
	
	@ParametersDelegate
	protected LearningConfig learningConfig;

	@ParametersDelegate
	protected MapperConfig mapperConfig;
	
	@ParametersDelegate
	protected TestRunnerConfig testRunnerConfig;

	@ParametersDelegate
	protected TimingProbeConfig timingProbeConfig;

	public StateFuzzerConfig() {
		learningConfig = new LearningConfig();
		mapperConfig = new MapperConfig();
		testRunnerConfig = new TestRunnerConfig();
		timingProbeConfig = new TimingProbeConfig();
	}

	public StateFuzzerConfig(LearningConfig learningConfig, MapperConfig mapperConfig,
							 TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig) {
		this.learningConfig = learningConfig;
		this.mapperConfig = mapperConfig;
		this.testRunnerConfig = testRunnerConfig;
		this.timingProbeConfig = timingProbeConfig;
	}

	public String getAlphabet() {
		return alphabet;
	}

	public String getOutput() {
		return output;
	}

	public LearningConfig getLearningConfig() {
		return learningConfig;
	}

	public MapperConfig getMapperConfig() {
		return mapperConfig;
	}

	public TestRunnerConfig getTestRunnerConfig() {
		return testRunnerConfig;
	}

	public TimingProbeConfig getTimingProbeConfig() {
		return timingProbeConfig;
	}
}
