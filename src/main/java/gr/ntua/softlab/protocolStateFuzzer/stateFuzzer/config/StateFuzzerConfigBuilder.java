package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

public interface StateFuzzerConfigBuilder {
    StateFuzzerClientConfig buildClientConfig();
    StateFuzzerServerConfig buildServerConfig();
}
