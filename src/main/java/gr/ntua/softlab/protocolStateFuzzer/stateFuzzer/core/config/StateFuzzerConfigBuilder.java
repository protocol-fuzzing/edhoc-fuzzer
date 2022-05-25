package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.core.config;

public interface StateFuzzerConfigBuilder {
    StateFuzzerClientConfig buildClientConfig();
    StateFuzzerServerConfig buildServerConfig();
}
