package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.AppProfile;
import org.eclipse.californium.edhoc.EdhocEndpointInfo;
import org.eclipse.californium.oscore.HashMapCtxDB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EdhocEndpointInfoPersistent extends EdhocEndpointInfo {
    protected Map<CBORObject, EdhocSessionPersistent> edhocSessionsPersistent;

    public EdhocEndpointInfoPersistent(
            HashMap<Integer, HashMap<Integer, CBORObject>> idCreds, HashMap<Integer, HashMap<Integer, CBORObject>> creds,
            HashMap<Integer, HashMap<Integer, OneKey>> keyPairs, HashMap<CBORObject, OneKey> peerPublicKeys,
            HashMap<CBORObject, CBORObject> peerCredentials, Map<CBORObject, EdhocSessionPersistent> edhocSessionsPersistent,
            Set<CBORObject> usedConnectionIds, List<Integer> supportedCipherSuites, Set<Integer> supportedEADs,
            HashMap<Integer, List<CBORObject>> eadProductionInput, int trustModel, HashMapCtxDB db, String oscoreUri,
            int OSCORE_REPLAY_WINDOW, int MAX_UNFRAGMENTED_SIZE, HashMap<String, AppProfile> appProfiles) {

        super(idCreds, creds, keyPairs, peerPublicKeys, peerCredentials, null, usedConnectionIds,
            supportedCipherSuites, supportedEADs, eadProductionInput, trustModel, db, oscoreUri, OSCORE_REPLAY_WINDOW,
            MAX_UNFRAGMENTED_SIZE, appProfiles);

        this.edhocSessionsPersistent = edhocSessionsPersistent;
    }

    public Map<CBORObject, EdhocSessionPersistent> getEdhocSessionsPersistent() {
        return edhocSessionsPersistent;
    }

    public String getOscoreUri() {
        return getUri();
    }
}
