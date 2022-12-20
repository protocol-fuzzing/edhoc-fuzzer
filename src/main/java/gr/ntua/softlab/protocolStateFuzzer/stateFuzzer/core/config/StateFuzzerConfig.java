package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.config.LearnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.core.config.TestRunnerEnabler;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeConfig;
import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeEnabler;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class StateFuzzerConfig extends ToolConfig implements
		StateFuzzerEnabler, TestRunnerEnabler, TimingProbeEnabler {

	@Parameter(names = "-output", description = "The directory in which results should be saved. The default is "
			+ "output/o_<timestamp>")
	protected String outputDir = null;
	
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
		this.learnerConfig = learnerConfig == null ? new LearnerConfig() : learnerConfig;
		this.testRunnerConfig = testRunnerConfig == null ? new TestRunnerConfig() : testRunnerConfig;
		this.timingProbeConfig = timingProbeConfig == null ? new TimingProbeConfig() : timingProbeConfig;
	}

	public String getOutputDir() {
		if (outputDir == null) {
			// initialize to default: output/o_<timestamp>
			String uniqueSubDir = "o_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
			outputDir = Path.of("output", uniqueSubDir).toString();
		}
		return outputDir;
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
