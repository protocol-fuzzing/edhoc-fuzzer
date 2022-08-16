package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient.ServerMapperConnector;
import org.eclipse.californium.oscore.HashMapCtxDB;

public class ServerMapperState extends EdhocMapperState {

    public ServerMapperState(EdhocMapperConfig edhocMapperConfig, ServerMapperConnector serverMapperConnector) {

        super(edhocMapperConfig, edhocMapperConfig.getHostCoapUri());

        // Prepare this session

        // large connection id, in order not to be equal with received C_I
        // in case of mapper using oscore context (for fuzzing only), but
        // the other peer does not derive oscore context
        byte[] connectionId = new byte[]{(byte) 255, (byte) 255, (byte) 255};
        usedConnectionIds.add(CBORObject.FromObject(connectionId));

        HashMapCtxDB oscoreDB = appProfiles.get(edhocMapperConfig.getEdhocCoapUri()).getUsedForOSCORE() ? db : null;

        edhocSessionPersistent = new EdhocSessionPersistent(edhocMapperConfig.isResponder(), edhocMapperConfig.isResponder(),
                authenticationMethod, connectionId, edhocEndpointInfoPersistent, oscoreDB);

        // Update edhocSessions
        edhocSessionsPersistent.put(CBORObject.FromObject(connectionId), edhocSessionPersistent);

        // Initialize server
        serverMapperConnector.initialize(new EdhocStackFactoryPersistent(edhocEndpointInfoPersistent,
                new MessageProcessorPersistent(this)));
    }

    @Override
    public boolean isCoapClient() {
        return false;
    }
}
