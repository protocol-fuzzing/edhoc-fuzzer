package gr.ntua.softlab.protocolStateFuzzer.timingProbe;

import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeConfig;
import gr.ntua.softlab.protocolStateFuzzer.timingProbe.config.TimingProbeEnabler;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetBuilder;
import gr.ntua.softlab.protocolStateFuzzer.learner.alphabet.AlphabetSerializerException;
import gr.ntua.softlab.protocolStateFuzzer.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.sul.WrappedSulBuilder;
import gr.ntua.softlab.protocolStateFuzzer.testRunner.ProbeTestRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TimingProbe {
    private static final Logger LOGGER = LogManager.getLogger(TimingProbe.class);
    protected TimingProbeConfig timingProbeConfig;
    protected AlphabetBuilder alphabetBuilder;
    protected ProbeTestRunner probeTestRunner = null;
    protected Integer lo, hi;

    public static String present(Map<String, Integer> map) {
        return map.keySet().stream()
                .map(key -> key + "=" + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    public TimingProbe(TimingProbeEnabler config, AlphabetBuilder alphabetBuilder,
                       MapperBuilder mapperBuilder, WrappedSulBuilder wrappedSulBuilder) throws IOException {
        this.timingProbeConfig = config.getTimingProbeConfig();
        this.alphabetBuilder = alphabetBuilder;

        if(isActive()) {
            probeTestRunner = new ProbeTestRunner(config, alphabetBuilder, mapperBuilder, wrappedSulBuilder);
        }
    }

    public void run() {
        if (!isActive() || probeTestRunner == null) {
            return;
        }

        try {
            if (isValid()) {
                Map<String, Integer> bestTimes = findDeterministicTimesValues();
                LOGGER.info(TimingProbe.present(bestTimes));
                alphabetBuilder.exportAlphabetToFile(timingProbeConfig.getProbeExport(), probeTestRunner.getAlphabet());
            }
        } catch (ProbeException | IOException | AlphabetSerializerException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        } finally {
            if (probeTestRunner != null) {
                probeTestRunner.terminate();
            }
        }
    }

    /*
     * findDeterministicTimeValues() finds the lowest values for the parameters
     * supplied in the -timingProbe parameter. This can be timeout, runWait or
     * an alphabet name (such as PSK_CLIENT_HELLO or HELLO_VERIFY_REQUEST). If
     * the parameter is an alphabet name, the extendedWait parameter is found.
     *
     * The search is done by first setting all parameters to the -probeHigh value
     * and then finding the first value leading to deterministic results using
     * a form of binary search.
     */
    public Map<String, Integer> findDeterministicTimesValues() throws IOException, ProbeException {
        Map<String, Integer> map = new HashMap<>();
        String[] cmds = timingProbeConfig.getProbeCmd().split(",");
        setAllTimingParameters(cmds);
        // do a control run, throw exception if non-deterministic
        if (probeTestRunner.isNonDeterministic(true)) throw new ProbeException("Non-determinism at max timing values");

        for (String cmd : cmds) {
            Integer bestTime;
            if (findLimits(cmd)) bestTime = timingProbeConfig.getProbeLo();
            else bestTime = binarySearch(cmd);
            map.put(cmd, bestTime);
            setTimingParameter(cmd, bestTime);
        }
        return map;
    }

    protected void setAllTimingParameters(String[] cmds) throws IllegalArgumentException {
        for (String cmd : cmds) {
            setTimingParameter(cmd, timingProbeConfig.getProbeHi());
        }
    }

    protected void setTimingParameter(String cmd, Integer time) throws IllegalArgumentException {
        if (cmd.contains("timeout")) {
            probeTestRunner.getSulDelegate().setTimeout(time);
        } else if (cmd.contains("runWait")) {
            Long runWait = time == null ? 0L : time;
            probeTestRunner.getSulDelegate().setRunWait(runWait);
        } else {
            for (AbstractInput in : probeTestRunner.getAlphabet()) {
                if (in.toString().contains(cmd)) in.setExtendedWait(time);
            }
        }
    }

    /*
     * findLimits sets hi to the first deterministic value encountered
     * (found by doubling hi each iteration) and lo to the last non-deterministic
     * returns true iff deterministic on the first try
     */
    protected boolean findLimits(String cmd) throws IOException {
        Integer probeLo = timingProbeConfig.getProbeLo();
        Integer probeHi = timingProbeConfig.getProbeHi();
        Integer probeMin = timingProbeConfig.getProbeMin();

        hi = probeLo;
        lo = probeLo;
        boolean keepSearching;
        if (cmd.contains("timeout") && hi == 0) keepSearching = true;
        else {
            setTimingParameter(cmd, hi);
            keepSearching = probeTestRunner.isNonDeterministic(false);
        }

        if (!keepSearching) return true;
        if (probeLo > 0) hi = probeLo;
        else {
            hi = probeMin;
            setTimingParameter(cmd, hi);
            keepSearching = probeTestRunner.isNonDeterministic(false);
        }
        while (keepSearching && hi < probeHi) {
            lo = hi;
            hi = hi * 2;
            setTimingParameter(cmd, hi);
            keepSearching = probeTestRunner.isNonDeterministic(false);
        }
        return false;
    }

    /*
     * binarySearch refines the search for deterministic value by using a binary search
     * [lo, hi] is the range of the search interval
     */
    protected Integer binarySearch(String cmd) throws IOException, IllegalArgumentException {
        while (hi - lo > timingProbeConfig.getProbeMin()) {
            Integer mid = lo + (hi - lo) / 2;
            setTimingParameter(cmd, mid);
            if (probeTestRunner.isNonDeterministic(false)) lo = mid;
            else hi = mid;
        }
        return hi;
    }

    public boolean isActive() {
        return timingProbeConfig.getProbeCmd() != null;
    }

    public boolean isValid() {
        String[] cmds = timingProbeConfig.getProbeCmd().split(",");
        for (String cmd : cmds) {
            if (!isValid(cmd)) return false;
        }
        return true;
    }

    public boolean isValid(String cmd) {
        if (cmd.contains("timeout") || cmd.contains("runWait")) return true;
        for (AbstractInput in : probeTestRunner.getAlphabet()) {
            if (in.toString().contains(cmd)) return true;
        }
        return false;
    }
}
