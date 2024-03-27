package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocEndpointInfoPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.CombinedMessageVersion;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.ProtocolVersion;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.AuthenticationConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.ManyFilesAuthenticationConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.TestVectorAuthenticationConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.CoapExchanger;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;
import com.upokecenter.cbor.CBORObject;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.AppProfile;
import org.eclipse.californium.edhoc.Constants;
import org.eclipse.californium.oscore.HashMapCtxDB;

import java.security.Security;
import java.util.*;

/** Adapted from test files EdhocClient / EdhocServer from edhoc repo */
public abstract class EdhocMapperState {

    // The authentication method to include in EDHOC message_1 (relevant only when
    // Initiator)
    protected int authenticationMethod;

    // Authentication credentials of this peer
    // At the top level, authentication credentials are sorted by key usage of the
    // authentication keys.
    // The outer map has label SIGNATURE_KEY or ECDH_KEY for distinguishing the two
    // key usages.
    // The asymmetric key pairs of this peer (one per supported curve)
    protected HashMap<Integer, HashMap<Integer, OneKey>> keyPairs = new HashMap<>();

    // The identifiers of the authentication credentials of this peer
    protected HashMap<Integer, HashMap<Integer, CBORObject>> idCreds = new HashMap<>();

    // The authentication credentials of this peer (one per supported curve)
    protected HashMap<Integer, HashMap<Integer, CBORObject>> creds = new HashMap<>();

    // Each element is the ID_CRED_X used for an authentication credential
    // associated to this peer
    protected Set<CBORObject> ownIdCreds = new HashSet<>();

    // Authentication credentials of the other peers
    // The map label is a CBOR Map used as ID_CRED_X
    protected HashMap<CBORObject, OneKey> peerPublicKeys = new HashMap<>();

    // Authentication credentials of other peers
    // The map label is a CBOR Map used as ID_CRED_X
    // The map value is a CBOR Byte String, with value the serialization of CRED_X
    protected HashMap<CBORObject, CBORObject> peerCredentials = new HashMap<>();

    // Existing EDHOC Sessions, including completed ones
    // The map label is C_X, i.e. the connection identifier offered to the other
    // peer, as a CBOR integer or byte string
    protected HashMap<CBORObject, EdhocSessionPersistent> edhocSessionsPersistent = new HashMap<>();

    // Each element is a used Connection Identifier offered to the other peers.
    // Connection Identifiers are stored as CBOR integers (if numeric) or as CBOR
    // byte strings (if binary)
    protected Set<CBORObject> usedConnectionIds = new HashSet<>();

    // List of supported cipher suites, in decreasing order of preference.
    protected List<Integer> supportedCipherSuites = new ArrayList<>();

    // Set of supported EAD items
    protected Set<Integer> supportedEADs = new HashSet<>();

    // The collection of application profiles - The lookup key is the full URI of
    // the EDHOC resource
    protected HashMap<String, AppProfile> appProfiles = new HashMap<>();

    // The database of OSCORE Security Contexts
    protected HashMapCtxDB db = new HashMapCtxDB();

    // The trust model for validating authentication credentials of other peers
    protected int trustModel;

    // The size of the Replay Window to use in an OSCORE Recipient Context
    protected int OSCORE_REPLAY_WINDOW = 32;

    // The size to consider for MAX_UNFRAGMENTED SIZE
    protected int MAX_UNFRAGMENTED_SIZE = 4096;

    protected EdhocMapperConfig edhocMapperConfig;

    protected EdhocSessionPersistent edhocSessionPersistent;

    protected EdhocEndpointInfoPersistent edhocEndpointInfoPersistent;

    protected CleanupTasks cleanupTasks;

    public EdhocMapperState(EdhocMapperConfig edhocMapperConfig, String edhocSessionUri, String oscoreUri,
            CleanupTasks cleanupTasks) {

        this.edhocMapperConfig = edhocMapperConfig;
        this.cleanupTasks = cleanupTasks;

        // Insert security providers
        Security.insertProviderAt(new EdDSASecurityProvider(), 1);
        Security.insertProviderAt(new BouncyCastleProvider(), 2);

        // Set authentication params
        AuthenticationConfig authenticationConfig = edhocMapperConfig.getAuthenticationConfig();
        boolean manyFilesAuthIsUsed = authenticationConfig.getManyFilesAuthenticationConfig().isUsed();
        if (manyFilesAuthIsUsed) {
            ManyFilesAuthenticationConfig config = authenticationConfig.getManyFilesAuthenticationConfig();
            this.authenticationMethod = config.getMapAuthenticationMethod();
            // Add the supported cipher suites in decreasing order of preference
            this.supportedCipherSuites.addAll(config.getMapSupportedCipherSuites());
        } else {
            TestVectorAuthenticationConfig config = authenticationConfig.getTestVectorAuthenticationConfig();
            this.authenticationMethod = config.getTestVector().getAuthenticationMethod();
            // Add the supported cipher suites in decreasing order of preference
            this.supportedCipherSuites.addAll(config.getTestVector().getCipherSuites());
        }

        this.trustModel = authenticationConfig.getTrustModel();

        // Set the application profile

        // Supported authentication methods
        Set<Integer> authMethods = new HashSet<>();
        for (int i = 0; i <= Constants.EDHOC_AUTH_METHOD_3; i++) {
            authMethods.add(i);
        }

        AppProfile appProfile = AppProfileBuilder.build(authMethods);
        appProfiles.put(edhocSessionUri, appProfile);

        // Prepare the set of information for this EDHOC endpoint
        edhocEndpointInfoPersistent = new EdhocEndpointInfoPersistent(
                idCreds, creds, keyPairs, peerPublicKeys, peerCredentials, edhocSessionsPersistent,
                usedConnectionIds, supportedCipherSuites, supportedEADs, null, trustModel, db, oscoreUri,
                OSCORE_REPLAY_WINDOW, MAX_UNFRAGMENTED_SIZE, appProfiles);

        // Set up the authentication credentials
        Authenticator authenticator;

        authenticator = manyFilesAuthIsUsed
                ? new ManyFilesAuthenticator(authenticationConfig, edhocEndpointInfoPersistent, ownIdCreds)
                : new TestVectorAuthenticator(authenticationConfig, edhocEndpointInfoPersistent, ownIdCreds,
                        edhocMapperConfig.isInitiator());

        authenticator.setupOwnAuthenticationCredentials();
        authenticator.setupPeerAuthenticationCredentials();

        // Prepare new session

        // add empty connection id to used ones so as not to be used
        // in case a new connection id is generated automatically
        usedConnectionIds.add(CBORObject.FromObject(new byte[0]));

        byte[] connectionId = edhocMapperConfig.getOwnConnectionId();
        usedConnectionIds.add(CBORObject.FromObject(connectionId));

        boolean isInitiator = edhocMapperConfig.isInitiator();

        // logically isClientInitiated when (isInitiator && isCoapClient()) ||
        // (!isInitiator && !isCoapClient())
        // which simplifies to isClientInitiated when isInitiator == isCoapClient()
        boolean isClientInitiated = isInitiator == isCoapClient();

        edhocSessionPersistent = new EdhocSessionPersistent(edhocSessionUri, isInitiator, isClientInitiated,
                authenticationMethod, connectionId, edhocEndpointInfoPersistent, null,
                db, new CoapExchanger(), edhocMapperConfig.useSessionReset(),
                edhocMapperConfig.getForceOscoreSenderId(), edhocMapperConfig.getForceOscoreRecipientId());

        // Update edhocSessions
        edhocSessionsPersistent.put(CBORObject.FromObject(connectionId), edhocSessionPersistent);

        if (edhocMapperConfig.getForceOscoreRecipientId() != null) {
            // forceRecipientId should point to the session, in order for the session to be
            // accessible from
            // the OSCORE context's recipient id as well
            edhocSessionsPersistent.put(CBORObject.FromObject(edhocMapperConfig.getForceOscoreRecipientId()),
                    edhocSessionPersistent);
        }
    }

    public EdhocMapperState initialize(EdhocMapperConnector edhocMapperConnector) {
        edhocMapperConnector.initialize(new EdhocStackFactoryPersistent(edhocEndpointInfoPersistent,
                new MessageProcessorPersistent(this)), edhocSessionPersistent.getCoapExchanger());
        return this;
    }

    public CleanupTasks getCleanupTasks() {
        return cleanupTasks;
    }

    public ProtocolVersion getProtocolVersion() {
        return edhocMapperConfig.getProtocolVersion();
    }

    public CombinedMessageVersion getCombinedMessageVersion() {
        return edhocMapperConfig.getCombinedMessageVersion();
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

    public EdhocMapperConfig getEdhocMapperConfig() {
        return edhocMapperConfig;
    }

    /** Specifies if this peer is the CoAP client or the CoAP server */
    public abstract boolean isCoapClient();

    /** Specifies if this peer should send messages with C_I or C_R prepended */
    public boolean sendWithPrependedCX() {
        // only coap client can send with prepended CX if it is enabled
        return isCoapClient() && edhocMapperConfig.useCXCorrelation();
    }

    /** Specifies if this peer should receive messages with C_I or C_R prepended */
    public boolean receiveWithPrependedCX() {
        // only coap server can receive with prepended CX if it is enabled
        return !isCoapClient() && edhocMapperConfig.useCXCorrelation();
    }
}
