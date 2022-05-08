package gr.ntua.softlab.protocolStateFuzzer.timingProbe;

import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeEnabler;

public interface TimingProbeBuilder {
    TimingProbe build(TimingProbeEnabler timingProbeEnabler);
}
