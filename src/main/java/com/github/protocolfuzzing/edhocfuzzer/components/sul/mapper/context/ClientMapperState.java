package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.context;

import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.ProtocolVersion;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.connectors.ClientMapperConnector;

public class ClientMapperState extends EdhocMapperState {

    public ClientMapperState(ProtocolVersion protocolVersion,  EdhocMapperConfig edhocMapperConfig,
                             ClientMapperConnector clientMapperConnector) {
        super(protocolVersion, edhocMapperConfig, edhocMapperConfig.getEdhocCoapUri(),
              edhocMapperConfig.getEdhocCoapUri(), clientMapperConnector);
    }

    @Override
    public boolean isCoapClient() {
        return true;
    }
}
