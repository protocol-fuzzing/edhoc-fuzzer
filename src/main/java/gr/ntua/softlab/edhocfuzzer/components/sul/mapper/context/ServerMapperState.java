package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.context;

import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.ProtocolVersion;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.connectors.ServerMapperConnector;

public class ServerMapperState extends EdhocMapperState {

    public ServerMapperState(ProtocolVersion protocolVersion, EdhocMapperConfig edhocMapperConfig,
                             ServerMapperConnector serverMapperConnector) {
        super(protocolVersion, edhocMapperConfig, edhocMapperConfig.getEdhocCoapUri(),
                edhocMapperConfig.getHostCoapUri(), serverMapperConnector);
    }

    @Override
    public boolean isCoapClient() {
        return false;
    }
}
