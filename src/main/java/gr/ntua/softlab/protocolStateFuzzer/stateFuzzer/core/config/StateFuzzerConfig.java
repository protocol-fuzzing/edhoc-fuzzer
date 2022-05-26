package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeEnabler;

public abstract class StateFuzzerConfig extends ToolConfig implements
		StateFuzzerEnabler, TestRunnerEnabler, TimingProbeEnabler {

	@Parameter(names = "-output", description = "The directory in which results should be saved")
	protected String output = "output";
	
	@ParametersDelegate
	protected LearnerConfig learnerConfig;

	@ParametersDelegate
	protected TestRunnerConfig testRunnerConfig;

	@ParametersDelegate
	protected TimingProbeConfig timingProbeConfig;

	public StateFuzzerConfig() {
		learnerConfig = new LearnerConfig();
		testRunnerConfig = new TestRunnerConfig();
		timingProbeConfig = new TimingProbeConfig();
	}

	public StateFuzzerConfig(LearnerConfig learnerConfig, TestRunnerConfig testRunnerConfig,
							 TimingProbeConfig timingProbeConfig) {
		this.learnerConfig = learnerConfig;
		this.testRunnerConfig = testRunnerConfig;
		this.timingProbeConfig = timingProbeConfig;
	}

	public String getOutput() {
		return output;
	}

	public LearnerConfig getLearnerConfig() {
		return learnerConfig;
	}

	public TestRunnerConfig getTestRunnerConfig() {
		return testRunnerConfig;
	}

	public TimingProbeConfig getTimingProbeConfig() {
		return timingProbeConfig;
	}
}
