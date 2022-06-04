package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

public class ConnectorToClient implements MapperConnector {
    @Override
    public void send(byte[] payload) {

    }

    @Override
    public byte[] receive() {
        return new byte[0];
    }
}
