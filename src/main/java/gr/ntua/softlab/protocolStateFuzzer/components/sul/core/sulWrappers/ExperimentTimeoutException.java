package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers;

import java.io.Serial;
import java.time.Duration;

public class ExperimentTimeoutException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	protected Duration duration;

	public ExperimentTimeoutException(Duration duration) {
		super("Experiment has exceeded the duration limit given: "
				+ duration.toString());
		this.duration = duration;
	}

	public Duration getDuration() {
		return duration;
	}
}
