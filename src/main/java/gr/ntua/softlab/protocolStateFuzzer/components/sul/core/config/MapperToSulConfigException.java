package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config;

public class MapperToSulConfigException extends RuntimeException {
    public MapperToSulConfigException() {
        super();
    }

    public MapperToSulConfigException(String message) {
        super(message);
    }

    public MapperToSulConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
