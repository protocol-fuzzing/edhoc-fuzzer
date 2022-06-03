package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers;

import java.io.Serial;

public class TestLimitReachedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private long testLimit;

    TestLimitReachedException(long testLimit) {
        super("Experiment has exceeded the duration limit given: " + testLimit);
        this.testLimit = testLimit;
    }

    public long getTestLimit() {
        return testLimit;
    }
}
