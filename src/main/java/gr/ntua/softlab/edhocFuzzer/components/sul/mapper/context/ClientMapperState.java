package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.californium.edhoc.*;
import org.eclipse.californium.oscore.HashMapCtxDB;

import com.upokecenter.cbor.CBORObject;

import net.i2p.crypto.eddsa.EdDSASecurityProvider;

import org.eclipse.californium.cose.OneKey;

public class ClientMapperState extends EdhocMapperState {

	private final static Provider EdDSA = new EdDSASecurityProvider();

	// The authentication method to include in EDHOC message_1 (relevant only when Initiator)
	private static int authenticationMethod = Constants.EDHOC_AUTH_METHOD_0;

	// The type of the authentication credential of this peer (same type for all its credentials)
	// Possible values: CRED_TYPE_CWT ; CRED_TYPE_CCS ; CRED_TYPE_X509
	private static int credType = Constants.CRED_TYPE_X509;

	// The type of the credential identifier of this peer (same type for all its credentials)
	// This will be the type of ID_CRED_R used in EDHOC message_2 or as ID_CRED_I in EDHOC message_3.
	// Possible values: ID_CRED_TYPE_KID ; ID_CRED_TYPE_CWT ; ID_CRED_TYPE_CCS ;
	//                  ID_CRED_TYPE_X5T ; ID_CRED_TYPE_X5U ; ID_CRED_TYPE_X5CHAIN
	private static int idCredType = Constants.ID_CRED_TYPE_X5T;

	// Authentication credentials of this peer
	//
	// At the top level, authentication credentials are sorted by key usage of the authentication keys.
	// The outer map has label SIGNATURE_KEY or ECDH_KEY for distinguishing the two key usages.
	//
	// The asymmetric key pairs of this peer (one per supported curve)
	private static HashMap<Integer, HashMap<Integer, OneKey>> keyPairs = new HashMap<>();

	// The identifiers of the authentication credentials of this peer
	private static HashMap<Integer, HashMap<Integer, CBORObject>> idCreds = new HashMap<>();

	// The authentication credentials of this peer (one per supported curve)
	private static HashMap<Integer, HashMap<Integer, CBORObject>> creds = new HashMap<>();

	// Each element is the ID_CRED_X used for an authentication credential associated to this peer
	private static Set<CBORObject> ownIdCreds = new HashSet<>();

	// Authentication credentials of the other peers
	//
	// The map label is a CBOR Map used as ID_CRED_X
	private static HashMap<CBORObject, OneKey> peerPublicKeys = new HashMap<>();

	// Authentication credentials of other peers
	//
	// The map label is a CBOR Map used as ID_CRED_X
	// The map value is a CBOR Byte String, with value the serialization of CRED_X
	private static HashMap<CBORObject, CBORObject> peerCredentials = new HashMap<>();

	// Existing EDHOC Sessions, including completed ones
	// The map label is C_X, i.e. the connection identifier offered to the other peer, as a CBOR integer or byte string
	private static HashMap<CBORObject, EdhocSession> edhocSessions = new HashMap<>();

	// Each element is a used Connection Identifier offered to the other peers.
	// Connection Identifiers are stored as CBOR integers (if numeric) or as CBOR byte strings (if binary)
	private static Set<CBORObject> usedConnectionIds = new HashSet<>();

	// List of supported cipher suites, in decreasing order of preference.
	private static List<Integer> supportedCipherSuites = new ArrayList<>();

	// The collection of application profiles - The lookup key is the full URI of the EDHOC resource
	private static HashMap<String, AppProfile> appProfiles = new HashMap<>();

	// The database of OSCORE Security Contexts
	private final static HashMapCtxDB db = new HashMapCtxDB();

	// The size of the Replay Window to use in an OSCORE Recipient Context
	private static final int OSCORE_REPLAY_WINDOW = 32;

	// The size to consider for MAX_UNFRAGMENTED SIZE
	private final static int MAX_UNFRAGMENTED_SIZE = 4096;

	protected EdhocSession edhocSession;
	protected EdhocEndpointInfo edhocEndpointInfo;

	public ClientMapperState(String edhocURI) {
		// edhocURI should be coap://localhost:port/.well-known/edhoc

		// Insert EdDSA security provider
		Security.insertProviderAt(EdDSA, 1);

		// Enable EDHOC stack with EDHOC and OSCORE layers
		EdhocCoapStackFactory.useAsDefault(db, edhocSessions, peerPublicKeys, peerCredentials, usedConnectionIds,
				OSCORE_REPLAY_WINDOW, MAX_UNFRAGMENTED_SIZE);

		// Add the supported cipher suites
		setupSupportedCipherSuites();

		// Set up the authentication credentials for this peer and the other peer
		setupOwnAuthenticationCredentials();

		// Set up the authentication credentials for the other peers
		setupPeerAuthenticationCredentials();

		// Set the application profile

		// Supported authentication methods
		Set<Integer> authMethods = new HashSet<>();
		for (int i = 0; i <= Constants.EDHOC_AUTH_METHOD_3; i++) authMethods.add(i);

		// Use of message_4 as expected to be sent by the Responder
		boolean useMessage4 = false;

		// Use of EDHOC for keying OSCORE
		boolean usedForOSCORE = true;

		// Supporting for the EDHOC+OSCORE request
		boolean supportCombinedRequest = true;

		AppProfile appProfile = new AppProfile(authMethods, useMessage4, usedForOSCORE, supportCombinedRequest);
		appProfiles.put(edhocURI, appProfile);

		// Specify the processor of External Authorization Data
		KissEDP edp = new KissEDP();

		// Prepare the set of information for this EDHOC endpoint
		edhocEndpointInfo = new EdhocEndpointInfo(
				idCreds, creds, keyPairs, peerPublicKeys, peerCredentials, edhocSessions,
				usedConnectionIds, supportedCipherSuites, db, edhocURI, OSCORE_REPLAY_WINDOW,
				MAX_UNFRAGMENTED_SIZE, appProfiles, edp
		);

		// Prepare this session
		edhocSession = MessageProcessor.createSessionAsInitiator(
				authenticationMethod, keyPairs, idCreds, creds, supportedCipherSuites,
				usedConnectionIds, appProfile, edp, db
		);
	}

	@Override
	public EdhocSession getEdhocSession() {
		return edhocSession;
	}

	@Override
	public void setEdhocSession(EdhocSession edhocSession) {
		this.edhocSession = edhocSession;
	}

	public EdhocEndpointInfo getEdhocEndpointInfo() {
		return edhocEndpointInfo;
	}

	private void setupSupportedCipherSuites() {

		// Add the supported cipher suites in decreasing order of preference
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_0);
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_1);
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_2);
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_3);

	}

	private void setupOwnAuthenticationCredentials () {}
	private void setupPeerAuthenticationCredentials () {}
}
