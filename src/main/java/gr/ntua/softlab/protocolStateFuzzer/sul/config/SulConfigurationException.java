package gr.ntua.softlab.protocolStateFuzzer.sul.config;

public class SulConfigurationException extends RuntimeException {
    public SulConfigurationException() {
        super();
    }

    public SulConfigurationException(String message) {
        super(message);
    }

    public SulConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
