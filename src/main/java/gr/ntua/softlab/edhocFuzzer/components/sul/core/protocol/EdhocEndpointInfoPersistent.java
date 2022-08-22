package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.AppProfile;
import org.eclipse.californium.edhoc.EDP;
import org.eclipse.californium.edhoc.EdhocEndpointInfo;
import org.eclipse.californium.oscore.HashMapCtxDB;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class EdhocEndpointInfoPersistent extends EdhocEndpointInfo {
    protected HashMap<CBORObject, EdhocSessionPersistent> edhocSessionsPersistent;

    public EdhocEndpointInfoPersistent(
            HashMap<Integer, HashMap<Integer, CBORObject>> idCreds, HashMap<Integer, HashMap<Integer, CBORObject>> creds,
            HashMap<Integer, HashMap<Integer, OneKey>> keyPairs, HashMap<CBORObject, OneKey> peerPublicKeys,
            HashMap<CBORObject, CBORObject> peerCredentials, HashMap<CBORObject, EdhocSessionPersistent> edhocSessionsPersistent,
            Set<CBORObject> usedConnectionIds, List<Integer> supportedCipherSuites, HashMapCtxDB db, String oscoreUri,
            int OSCORE_REPLAY_WINDOW, int MAX_UNFRAGMENTED_SIZE, HashMap<String, AppProfile> appProfiles, EDP edp) {

        super(idCreds, creds, keyPairs, peerPublicKeys, peerCredentials, null, usedConnectionIds,
                supportedCipherSuites, db, oscoreUri, OSCORE_REPLAY_WINDOW, MAX_UNFRAGMENTED_SIZE, appProfiles, edp);

        this.edhocSessionsPersistent = edhocSessionsPersistent;
    }

    public HashMap<CBORObject, EdhocSessionPersistent> getEdhocSessionsPersistent() {
        return edhocSessionsPersistent;
    }

    public String getOscoreUri() {
        return getUri();
    }
}
