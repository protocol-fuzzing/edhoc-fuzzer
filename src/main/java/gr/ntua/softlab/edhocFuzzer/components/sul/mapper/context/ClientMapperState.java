package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulServer.ClientMapperConnector;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.ProtocolVersion;

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
