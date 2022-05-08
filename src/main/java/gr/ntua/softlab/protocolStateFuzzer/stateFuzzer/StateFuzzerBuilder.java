package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer;

import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerConfig;

public interface StateFuzzerBuilder {
    StateFuzzer build(StateFuzzerConfig stateFuzzerConfig);
}
