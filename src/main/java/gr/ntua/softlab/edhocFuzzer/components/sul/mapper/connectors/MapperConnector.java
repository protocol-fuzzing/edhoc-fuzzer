package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

public interface MapperConnector {
    void send(byte[] payload);
    byte[] receive();
}
