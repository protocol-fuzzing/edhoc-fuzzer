package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulServer.ClientMapperConnector;
import org.eclipse.californium.oscore.HashMapCtxDB;

public class ClientMapperState extends EdhocMapperState {

	public ClientMapperState(EdhocMapperConfig edhocMapperConfig, ClientMapperConnector clientMapperConnector) {

		super(edhocMapperConfig, edhocMapperConfig.getEdhocCoapUri());

		// Prepare this session

		// large connection id, in order not to be equal with received C_R
		// in case of mapper using oscore context (for fuzzing only), but
		// the other peer does not derive oscore context
		byte[] connectionId = new byte[]{(byte) 255, (byte) 255, (byte) 255};
		usedConnectionIds.add(CBORObject.FromObject(connectionId));

		HashMapCtxDB oscoreDB = appProfiles.get(edhocMapperConfig.getEdhocCoapUri()).getUsedForOSCORE() ? db : null;

		edhocSessionPersistent = new EdhocSessionPersistent(edhocMapperConfig.isInitiator(), edhocMapperConfig.isInitiator(),
				authenticationMethod, connectionId, edhocEndpointInfoPersistent, oscoreDB);

		// Update edhocSessions
		edhocSessionsPersistent.put(CBORObject.FromObject(connectionId), edhocSessionPersistent);

		// Initialize client
		clientMapperConnector.initialize(new EdhocStackFactoryPersistent(edhocEndpointInfoPersistent,
				new MessageProcessorPersistent(this)));
	}

	@Override
	public boolean isCoapClient() {
		return true;
	}
}
