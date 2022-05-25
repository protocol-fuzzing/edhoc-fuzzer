package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe;

import gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.testRunner.timingProbe.config.TimingProbeEnabler;

public interface TimingProbeBuilder {
    TimingProbe build(TimingProbeEnabler timingProbeEnabler);
}
