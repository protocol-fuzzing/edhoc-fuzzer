package gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe;

import gr.ntua.softlab.protocolStateFuzzer.testRunner.timingProbe.config.TimingProbeEnabler;

public interface TimingProbeBuilder {
    TimingProbe build(TimingProbeEnabler timingProbeEnabler);
}
