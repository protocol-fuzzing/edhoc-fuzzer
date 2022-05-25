package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.config.TimingProbeEnabler;

public abstract class StateFuzzerConfig extends ToolConfig implements
		StateFuzzerEnabler, TestRunnerEnabler, TimingProbeEnabler {

	@Parameter(names = "-output", description = "The directory in which results should be saved")
	protected String output = "output";
	
	@ParametersDelegate
	protected LearnerConfig learnerConfig;

	@ParametersDelegate
	protected MapperConfig mapperConfig;
	
	@ParametersDelegate
	protected TestRunnerConfig testRunnerConfig;

	@ParametersDelegate
	protected TimingProbeConfig timingProbeConfig;

	public StateFuzzerConfig() {
		learnerConfig = new LearnerConfig();
		mapperConfig = new MapperConfig();
		testRunnerConfig = new TestRunnerConfig();
		timingProbeConfig = new TimingProbeConfig();
	}

	public StateFuzzerConfig(LearnerConfig learnerConfig, MapperConfig mapperConfig,
							 TestRunnerConfig testRunnerConfig, TimingProbeConfig timingProbeConfig) {
		this.learnerConfig = learnerConfig;
		this.mapperConfig = mapperConfig;
		this.testRunnerConfig = testRunnerConfig;
		this.timingProbeConfig = timingProbeConfig;
	}

	public String getOutput() {
		return output;
	}

	public LearnerConfig getLearnerConfig() {
		return learnerConfig;
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
