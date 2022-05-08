package gr.ntua.softlab.protocolStateFuzzer.timingProbe.config;

import com.beust.jcommander.Parameter;

public class TimingProbeConfig {
    @Parameter(names = "-timingProbe", required = false, description = "Probe for timing values by testing for non-determinism")
    protected String probeCmd = null;

    @Parameter(names = "-probeMin", required = false, description = "Minimum timing value for probe")
    protected Integer probeMin = 10;

    @Parameter(names = "-probeLow", required = false, description = "Lowest timing value for probe")
    protected Integer probeLo = 0;

    @Parameter(names = "-probeHigh", required = false, description = "Highest timing value for probe")
    protected Integer probeHi = 1000;

    @Parameter(names = "-probeExport", required = false, description = "Output file for the modified alphabet")
    protected String probeExport = null;

    public String getProbeCmd() {
        return probeCmd;
    }

    public Integer getProbeMin() {
        return probeMin;
    }

    public Integer getProbeLo() {
        return probeLo;
    }

    public Integer getProbeHi() {
        return probeHi;
    }

    public String getProbeExport() {
        return probeExport;
    }
}
