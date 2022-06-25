package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

public interface EdhocMapperConnector {
    void send(byte[] payload);
    byte[] receive();
    void setTimeout(Long timeout);
}
