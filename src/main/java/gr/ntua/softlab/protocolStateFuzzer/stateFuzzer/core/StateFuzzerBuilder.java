package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core;

import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config.StateFuzzerEnabler;

public interface StateFuzzerBuilder {
    StateFuzzer build(StateFuzzerEnabler stateFuzzerEnabler);
}
