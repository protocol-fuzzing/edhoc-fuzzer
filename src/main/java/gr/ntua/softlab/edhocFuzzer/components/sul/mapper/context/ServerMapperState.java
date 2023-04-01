package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.ProtocolVersion;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient.ServerMapperConnector;

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
