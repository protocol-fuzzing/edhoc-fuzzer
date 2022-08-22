package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulServer.ClientMapperConnector;

public class ClientMapperState extends EdhocMapperState {

	public ClientMapperState(EdhocMapperConfig edhocMapperConfig, ClientMapperConnector clientMapperConnector) {
		super(edhocMapperConfig, edhocMapperConfig.getEdhocCoapUri(), edhocMapperConfig.getEdhocCoapUri(),
				clientMapperConnector);
	}

	@Override
	public boolean isCoapClient() {
		return true;
	}
}
