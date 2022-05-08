package gr.ntua.softlab.protocolStateFuzzer.learner.config;

import com.beust.jcommander.IStringConverter;

import java.time.Duration;

public class DurationConverter implements IStringConverter<Duration> {

	@Override
	public Duration convert(String value) {
		return Duration.parse(value);
	}

}
