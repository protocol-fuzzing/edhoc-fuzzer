package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer;

import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config.StateFuzzerEnabler;

public interface StateFuzzerBuilder {
    StateFuzzer build(StateFuzzerEnabler stateFuzzerEnabler);
}
