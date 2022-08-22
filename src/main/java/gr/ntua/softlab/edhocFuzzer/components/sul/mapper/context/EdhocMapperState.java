package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocEndpointInfoPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient.CoapExchangeWrapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.State;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.AppProfile;
import org.eclipse.californium.edhoc.Constants;
import org.eclipse.californium.edhoc.KissEDP;
import org.eclipse.californium.oscore.HashMapCtxDB;

import java.security.Security;
import java.util.*;

/** Adapted from test files EdhocClient / EdhocServer from edhoc repo */
public abstract class EdhocMapperState implements State {
    // The authentication method to include in EDHOC message_1 (relevant only when Initiator)
    protected int authenticationMethod;

    // The type of the authentication credential of this peer (same type for all its credentials)
    // Possible values: CRED_TYPE_CWT ; CRED_TYPE_CCS ; CRED_TYPE_X509
    protected int credType;

    // The type of the credential identifier of this peer (same type for all its credentials)
    // This will be the type of ID_CRED_R used in EDHOC message_2 or as ID_CRED_I in EDHOC message_3.
    // Possible values: ID_CRED_TYPE_KID ; ID_CRED_TYPE_CWT ; ID_CRED_TYPE_CCS ;
    //                  ID_CRED_TYPE_X5T ; ID_CRED_TYPE_X5U ; ID_CRED_TYPE_X5CHAIN
    protected int idCredType;

    // Authentication credentials of this peer
    // At the top level, authentication credentials are sorted by key usage of the authentication keys.
    // The outer map has label SIGNATURE_KEY or ECDH_KEY for distinguishing the two key usages.
    // The asymmetric key pairs of this peer (one per supported curve)
    protected HashMap<Integer, HashMap<Integer, OneKey>> keyPairs = new HashMap<>();

    // The identifiers of the authentication credentials of this peer
    protected HashMap<Integer, HashMap<Integer, CBORObject>> idCreds = new HashMap<>();

    // The authentication credentials of this peer (one per supported curve)
    protected HashMap<Integer, HashMap<Integer, CBORObject>> creds = new HashMap<>();

    // Each element is the ID_CRED_X used for an authentication credential associated to this peer
    protected Set<CBORObject> ownIdCreds = new HashSet<>();

    // Authentication credentials of the other peers
    // The map label is a CBOR Map used as ID_CRED_X
    protected HashMap<CBORObject, OneKey> peerPublicKeys = new HashMap<>();

    // Authentication credentials of other peers
    // The map label is a CBOR Map used as ID_CRED_X
    // The map value is a CBOR Byte String, with value the serialization of CRED_X
    protected HashMap<CBORObject, CBORObject> peerCredentials = new HashMap<>();

    // Existing EDHOC Sessions, including completed ones
    // The map label is C_X, i.e. the connection identifier offered to the other peer, as a CBOR integer or byte string
    protected HashMap<CBORObject, EdhocSessionPersistent> edhocSessionsPersistent = new HashMap<>();

    // Each element is a used Connection Identifier offered to the other peers.
    // Connection Identifiers are stored as CBOR integers (if numeric) or as CBOR byte strings (if binary)
    protected Set<CBORObject> usedConnectionIds = new HashSet<>();

    // List of supported cipher suites, in decreasing order of preference.
    protected List<Integer> supportedCipherSuites = new ArrayList<>();

    // The collection of application profiles - The lookup key is the full URI of the EDHOC resource
    protected HashMap<String, AppProfile> appProfiles = new HashMap<>();

    // The database of OSCORE Security Contexts
    protected HashMapCtxDB db = new HashMapCtxDB();

    // The size of the Replay Window to use in an OSCORE Recipient Context
    protected int OSCORE_REPLAY_WINDOW = 32;

    // The size to consider for MAX_UNFRAGMENTED SIZE
    protected int MAX_UNFRAGMENTED_SIZE = 4096;

    protected EdhocSessionPersistent edhocSessionPersistent;

    protected EdhocEndpointInfoPersistent edhocEndpointInfoPersistent;

    public EdhocMapperState(EdhocMapperConfig edhocMapperConfig, String edhocSessionUri, String oscoreUri,
                            EdhocMapperConnector edhocMapperConnector) {

        // Insert security providers
        Security.insertProviderAt(new EdDSASecurityProvider(), 1);
        Security.insertProviderAt(new BouncyCastleProvider(), 2);

        // Set authentication params
        this.authenticationMethod = edhocMapperConfig.getAuthenticationConfig().getMapAuthenticationMethod();
        this.credType = edhocMapperConfig.getAuthenticationConfig().getMapCredType();
        this.idCredType = edhocMapperConfig.getAuthenticationConfig().getMapIdCredType();

        // Add the supported cipher suites in decreasing order of preference
        this.supportedCipherSuites.addAll(edhocMapperConfig.getAuthenticationConfig().getMapSupportedCipherSuites());

        // Set the application profile

        // Supported authentication methods
        Set<Integer> authMethods = new HashSet<>();
        for (int i = 0; i <= Constants.EDHOC_AUTH_METHOD_3; i++) {
            authMethods.add(i);
        }

        AppProfile appProfile = AppProfileBuilder.build(authMethods, edhocMapperConfig.getAppProfileMode());
        appProfiles.put(edhocSessionUri, appProfile);

        // Specify the processor of External Authorization Data
        KissEDP edp = new KissEDP();

        // Prepare the set of information for this EDHOC endpoint
        edhocEndpointInfoPersistent = new EdhocEndpointInfoPersistent(
                idCreds, creds, keyPairs, peerPublicKeys, peerCredentials, edhocSessionsPersistent,
                usedConnectionIds, supportedCipherSuites, db, oscoreUri,
                OSCORE_REPLAY_WINDOW, MAX_UNFRAGMENTED_SIZE, appProfiles, edp
        );

        // Set up the authentication credentials
        Authenticator authenticator = new Authenticator(authenticationMethod, credType, idCredType,
                edhocEndpointInfoPersistent, ownIdCreds, edhocMapperConfig.getAuthenticationConfig());

        authenticator.setupOwnAuthenticationCredentials();
        authenticator.setupPeerAuthenticationCredentials();

        // Prepare new session

        // large connection id, in order not to be equal with received C_I / C_R
        // in case of mapper using oscore context (for fuzzing only), but
        // the other peer does not derive oscore context
        byte[] connectionId = new byte[]{(byte) 255, (byte) 255, (byte) 255};
        usedConnectionIds.add(CBORObject.FromObject(connectionId));

        HashMapCtxDB oscoreDB = appProfile.getUsedForOSCORE() ? db : null;

        boolean isInitiator = edhocMapperConfig.isInitiator();

        // logically isClientInitiated when (isInitiator && isCoapClient()) || (!isInitiator && !isCoapClient())
        // which simplifies to isClientInitiated when isInitiator == isCoapClient()
        boolean isClientInitiated = isInitiator == isCoapClient();

        edhocSessionPersistent = new EdhocSessionPersistent(edhocSessionUri, isInitiator, isClientInitiated,
                authenticationMethod, connectionId, edhocEndpointInfoPersistent, oscoreDB, new CoapExchangeWrapper());

        // Update edhocSessions
        edhocSessionsPersistent.put(CBORObject.FromObject(connectionId), edhocSessionPersistent);

        // Initialize connector
        edhocMapperConnector.initialize(new EdhocStackFactoryPersistent(edhocEndpointInfoPersistent,
                new MessageProcessorPersistent(this)), edhocSessionPersistent.getCoapExchangeWrapper());
    }

    public EdhocSessionPersistent getEdhocSessionPersistent() {
        return edhocSessionPersistent;
    }

    public void setEdhocSessionPersistent(EdhocSessionPersistent edhocSessionPersistent) {
        this.edhocSessionPersistent = edhocSessionPersistent;
    }

    public EdhocEndpointInfoPersistent getEdhocEndpointInfoPersistent() {
        return edhocEndpointInfoPersistent;
    }

    public Set<CBORObject> getOwnIdCreds() {
        return ownIdCreds;
    }

    /** Specifies if this peer is the CoAP client or the CoAP server */
    public abstract boolean isCoapClient();

}
