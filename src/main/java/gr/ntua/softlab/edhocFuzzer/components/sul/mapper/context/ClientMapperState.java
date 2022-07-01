package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import org.eclipse.californium.edhoc.*;
import org.eclipse.californium.oscore.HashMapCtxDB;

import com.upokecenter.cbor.CBORObject;

import net.i2p.crypto.eddsa.EdDSASecurityProvider;

import org.eclipse.californium.cose.OneKey;

import static net.i2p.crypto.eddsa.Utils.hexToBytes;

/** Adapted from test file EdhocClient from edhoc repo */
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
		HashMapCtxDB oscoreDB = (appProfile.getUsedForOSCORE()) ? db : null;
		byte[] connectionId = Util.getConnectionId(usedConnectionIds, oscoreDB, null);
		edhocSession = new EdhocSessionPersistent(
				true, true, authenticationMethod, connectionId,
				keyPairs, idCreds, creds, supportedCipherSuites, appProfile, edp, oscoreDB);

		// update edhocSessions
		edhocSessions.put(CBORObject.FromObject(connectionId), edhocSession);
	}

	@Override
	public EdhocSession getEdhocSession() {
		return edhocSession;
	}

	@Override
	public void setEdhocSession(EdhocSession edhocSession) {
		this.edhocSession = edhocSession;
	}

	@Override
	public EdhocEndpointInfo getEdhocEndpointInfo() {
		return edhocEndpointInfo;
	}

	public Set<CBORObject> getOwnIdCreds() {
		return ownIdCreds;
	}

	private void setupSupportedCipherSuites() {

		// Add the supported cipher suites in decreasing order of preference
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_0);
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_1);
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_2);
		supportedCipherSuites.add(Constants.EDHOC_CIPHER_SUITE_3);

	}

	private static void setupOwnAuthenticationCredentials () {

		byte[] privateKeyBinary;
		byte[] publicKeyBinary;
		byte[] publicKeyBinaryY;
		byte[] serializedCert = null;

		keyPairs.put(Constants.SIGNATURE_KEY, new HashMap<>());
		keyPairs.put(Constants.ECDH_KEY, new HashMap<>());
		creds.put(Constants.SIGNATURE_KEY, new HashMap<>());
		creds.put(Constants.ECDH_KEY, new HashMap<>());
		idCreds.put(Constants.SIGNATURE_KEY, new HashMap<>());
		idCreds.put(Constants.ECDH_KEY, new HashMap<>());

		// A single type will be used for all these authentication credentials.
		// A single type will be used for identifiers of the authentication credentials.

		// The subject name used for the identity key of this peer
		String subjectName = "";


		// Add one authentication credential for curve Ed25519 and one for curve X25519

		if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_0) ||
				supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_1)) {


			// Curve Ed25519

			OneKey keyPairEd25519;
			byte[] credEd25519 = null;
			CBORObject idCredEd25519 = null;
			CBORObject ccsObjectEd25519 = null;

			// If the type of credential identifier is 'kid', use 0x00,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x00
			byte[] kidEd25519 = new byte[] {(byte) 0x00};

			// Build the key pair

			privateKeyBinary = hexToBytes("2ffce7a0b2b825d397d0cb54f746e3da3f27596ee06b5371481dc0e012bc34d7");
			publicKeyBinary = hexToBytes("38e5d54563c2b6a4ba26f3015f61bb706e5c2efdb556d2e1690b97fc3c6de149");
			keyPairEd25519 = SharedSecretCalculation.buildEd25519OneKey(privateKeyBinary, publicKeyBinary);

			// Build CRED

			switch (credType) {
				case Constants.CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.CRED_TYPE_CCS:
					System.out.print("My   ");
					CBORObject idCredKidCbor = CBORObject.FromObject(kidEd25519);
					ccsObjectEd25519 = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(keyPairEd25519,
							subjectName, idCredKidCbor));

					// These serializations have to be prepared manually, in order to ensure that
					// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
					credEd25519 = hexToBytes("A2026008A101A40101024100200621582038E5D54563C2B6A"
							+ "4BA26F3015F61BB706E5C2EFDB556D2E1690B97FC3C6DE149");
					break;
				case Constants.CRED_TYPE_X509:
					// The x509 certificate of this peer
					serializedCert = hexToBytes("5413204c3ebc3428a6cf57e24c9def59651770449bce7ec6561e52433aa55e71f1fa34b22a9c"
							+ "a4a1e12924eae1d1766088098449cb848ffc795f88afc49cbe8afdd1ba009f21675e8f"
							+ "6c77a4a2c30195601f6f0a0852978bd43d28207d44486502ff7bdda6");

					// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
					credEd25519 = CBORObject.FromObject(serializedCert).EncodeToBytes();
					break;
			}

			// Build ID_CRED

			switch (idCredType) {
				case Constants.ID_CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.ID_CRED_TYPE_CCS:
					idCredEd25519 = Util.buildIdCredKccs(ccsObjectEd25519);
					break;
				case Constants.ID_CRED_TYPE_KID:
					idCredEd25519 = Util.buildIdCredKid(kidEd25519);
					break;
				case Constants.ID_CRED_TYPE_X5CHAIN:
					idCredEd25519 = Util.buildIdCredX5chain(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5T:
					idCredEd25519 = Util.buildIdCredX5t(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5U:
					idCredEd25519 = Util.buildIdCredX5u("http://example.repo.com/hostA-x509-Ed25519");
					break;
			}

			// Add the key pair, CRED and ID_CRED to the respective collections
			keyPairs.get(Constants.SIGNATURE_KEY).
					put(Constants.CURVE_Ed25519, keyPairEd25519);
			creds.get(Constants.SIGNATURE_KEY).
					put(Constants.CURVE_Ed25519, CBORObject.FromObject(credEd25519));
			idCreds.get(Constants.SIGNATURE_KEY).
					put(Constants.CURVE_Ed25519, idCredEd25519);

			// Add this ID_CRED to the whole collection of ID_CRED_X for this peer
			ownIdCreds.add(idCredEd25519);


			// Curve X25519

			OneKey keyPairX25519;
			byte[] credX25519 = null;
			CBORObject idCredX25519 = null;
			CBORObject ccsObjectX25519 = null;

			// If the type of credential identifier is 'kid', use 0x01,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x01
			byte[] kidX25519 = new byte[] {(byte) 0x01};

			// Build the key pair

			privateKeyBinary = hexToBytes("2bbea655c23371c329cfbd3b1f02c6c062033837b8b59099a4436f666081b08e");
			publicKeyBinary = hexToBytes("2c440cc121f8d7f24c3b0e41aedafe9caa4f4e7abb835ec30f1de88adb96ff71");
			keyPairX25519 = SharedSecretCalculation.buildCurve25519OneKey(privateKeyBinary, publicKeyBinary);

			// Build CRED

			switch (credType) {
				case Constants.CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.CRED_TYPE_CCS:
					System.out.print("My   ");
					CBORObject idCredKidCbor = CBORObject.FromObject(kidX25519);
					ccsObjectX25519 = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(keyPairX25519,
							subjectName, idCredKidCbor));

					// These serializations have to be prepared manually, in order to ensure that
					// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
					credX25519 = hexToBytes("A2026008A101A4010102410120042158202C440CC121F8D7F24C3B"
							+ "0E41AEDAFE9CAA4F4E7ABB835EC30F1DE88ADB96FF71");
					break;
				case Constants.CRED_TYPE_X509:
					// The x509 certificate of this peer
					serializedCert = hexToBytes("5413204c3ebc3428a6cf57e24c9def59651770449bce7ec6561e52433"
							+ "aa55e71f1fa34b22a9ca4a1e12924eae1d1766088098449cb848ffc795f88afc49cbe8afdd1ba0"
							+ "09f21675e8f6c77a4a2c30195601f6f0a0852978bd43d28207d44486502ff7bdda7");

					// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
					credX25519 = CBORObject.FromObject(serializedCert).EncodeToBytes();
					break;
			}

			// Build ID_CRED

			switch (idCredType) {
				case Constants.ID_CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.ID_CRED_TYPE_CCS:
					idCredX25519 = Util.buildIdCredKccs(ccsObjectX25519);
					break;
				case Constants.ID_CRED_TYPE_KID:
					idCredX25519 = Util.buildIdCredKid(kidX25519);
					break;
				case Constants.ID_CRED_TYPE_X5CHAIN:
					idCredX25519 = Util.buildIdCredX5chain(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5T:
					idCredX25519 = Util.buildIdCredX5t(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5U:
					idCredX25519 = Util.buildIdCredX5u("http://example.repo.com/hostA-x509-X25519");
					break;
			}

			// Add the key pair, CRED and ID_CRED to the respective collections
			keyPairs.get(Constants.ECDH_KEY).
					put(Constants.CURVE_X25519, keyPairX25519);
			creds.get(Constants.ECDH_KEY).
					put(Constants.CURVE_X25519, CBORObject.FromObject(credX25519));
			idCreds.get(Constants.ECDH_KEY).
					put(Constants.CURVE_X25519, idCredX25519);

			// Add this ID_CRED to the whole collection of ID_CRED_X for this peer
			ownIdCreds.add(idCredX25519);

		}


		// Add two authentication credentials for curve P-256 (one for signing only, one for ECDH only)
		if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_2) ||
				supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_3)) {

			// Signing authentication credential

			OneKey keyPairP256;
			byte[] credP256 = null;
			CBORObject idCredP256 = null;
			CBORObject ccsObjectP256 = null;

			// If the type of credential identifier is 'kid', use 0x02,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x02
			byte[] kidP256 = new byte[] {(byte) 0x02};

			// Build the key pair

			privateKeyBinary = hexToBytes("04f347f2bead699adb247344f347f2bdac93c7f2bead6a9d2a9b24754a1e2b62");
			publicKeyBinary = hexToBytes("cd4177ba62433375ede279b5e18e8b91bc3ed8f1e174474a26fc0edb44ea5373");
			publicKeyBinaryY = hexToBytes("A0391DE29C5C5BADDA610D4E301EAAA18422367722289CD18CBE6624E89B9CFD");
			keyPairP256 =  SharedSecretCalculation.buildEcdsa256OneKey(privateKeyBinary, publicKeyBinary,
					publicKeyBinaryY);

			// Build CRED
			switch (credType) {
				case Constants.CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.CRED_TYPE_CCS:
					System.out.print("My   ");
					CBORObject idCredKidCbor = CBORObject.FromObject(kidP256);
					ccsObjectP256 = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(keyPairP256, subjectName,
							idCredKidCbor));

					// These serializations have to be prepared manually, in order to ensure that
					// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
					credP256 = hexToBytes("A2026008A101A501020241022001215820CD4177BA62433375EDE279B"
							+ "5E18E8B91BC3ED8F1E174474A26FC0EDB44EA5373225820A0391DE29C5C5BADDA610D4E3"
							+ "01EAAA18422367722289CD18CBE6624E89B9CFD");
					break;
				case Constants.CRED_TYPE_X509:
					// The x509 certificate of this peer
					serializedCert = hexToBytes("5413204c3ebc3428a6cf57e24c9def59651770449bce7ec6561e524"
							+ "33aa55e71f1fa34b22a9ca4a1e12924eae1d1766088098449cb848ffc795f88afc49cbe8afdd"
							+ "1ba009f21675e8f6c77a4a2c30195601f6f0a0852978bd43d28207d44486502ff7bdda8");

					// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
					credP256 = CBORObject.FromObject(serializedCert).EncodeToBytes();
					break;
			}

			// Build ID_CRED

			switch (idCredType) {
				case Constants.ID_CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.ID_CRED_TYPE_CCS:
					idCredP256 = Util.buildIdCredKccs(ccsObjectP256);
					break;
				case Constants.ID_CRED_TYPE_KID:
					idCredP256 = Util.buildIdCredKid(kidP256);
					break;
				case Constants.ID_CRED_TYPE_X5CHAIN:
					idCredP256 = Util.buildIdCredX5chain(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5T:
					idCredP256 = Util.buildIdCredX5t(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5U:
					idCredP256 = Util.buildIdCredX5u("http://example.repo.com/hostA-x509-P256-signing");
					break;
			}

			// Add the key pair, CRED and ID_CRED to the respective collections
			keyPairs.get(Constants.SIGNATURE_KEY).
					put(Constants.CURVE_P256, keyPairP256);
			creds.get(Constants.SIGNATURE_KEY).
					put(Constants.CURVE_P256, CBORObject.FromObject(credP256));
			idCreds.get(Constants.SIGNATURE_KEY).
					put(Constants.CURVE_P256, idCredP256);

			// Add this ID_CRED to the whole collection of ID_CRED_X for this peer
			ownIdCreds.add(idCredP256);

			// ECDH authentication credential

			OneKey keyPairP256dh;
			byte[] credP256dh = null;
			CBORObject idCredP256dh = null;
			CBORObject ccsObjectP256dh = null;

			// If the type of credential identifier is 'kid', use 0x03,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x03
			byte[] kidP256dh = new byte[] {(byte) 0x03};

			// Build the key pair

			privateKeyBinary = hexToBytes("fb13adeb6518cee5f88417660841142e830a81fe334380a953406a1305e8706b");
			publicKeyBinary = hexToBytes("ac75e9ece3e50bfc8ed60399889522405c47bf16df96660a41298cb4307f7eb6");
			publicKeyBinaryY = hexToBytes("6e5de611388a4b8a8211334ac7d37ecb52a387d257e6db3c2a93df21ff3affc8");
			keyPairP256dh =  SharedSecretCalculation.buildEcdsa256OneKey(privateKeyBinary, publicKeyBinary,
					publicKeyBinaryY);

			// Build CRED
			switch (credType) {
				case Constants.CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.CRED_TYPE_CCS:
					System.out.print("My   ");
					CBORObject idCredKidCbor = CBORObject.FromObject(kidP256dh);
					ccsObjectP256dh = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(keyPairP256dh,
							subjectName, idCredKidCbor));

					// These serializations have to be prepared manually, in order to ensure that
					// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
					credP256dh = hexToBytes("A2026008A101A501020241032001215820AC75E9ECE3E50BFC8ED60399889522405C47BF"
							+ "16DF96660A41298CB4307F7EB62258206E5DE611388A4B8A8211334AC7D37ECB52A387D257E6DB3C2A93DF21F"
							+ "F3AFFC8");
					break;
				case Constants.CRED_TYPE_X509:
					// The x509 certificate of this peer
					serializedCert = hexToBytes("7713204c3ebc3428a6cf57e24c9def59651770449bce7ec6561e52433aa55e71f1fa"
							+ "34b22a9ca4a1e12924eae1d1766088098449cb848ffc795f88afc49cbe8afdd1ba009f21675e8f6c77a4a2c30"
							+ "195601f6f0a0852978bd43d28207d44486502ff7bdda8");

					// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
					credP256dh = CBORObject.FromObject(serializedCert).EncodeToBytes();
					break;
			}

			// Build ID_CRED

			switch (idCredType) {
				case Constants.ID_CRED_TYPE_CWT:
					// TODO
					break;
				case Constants.ID_CRED_TYPE_CCS:
					idCredP256dh = Util.buildIdCredKccs(ccsObjectP256dh);
					break;
				case Constants.ID_CRED_TYPE_KID:
					idCredP256dh = Util.buildIdCredKid(kidP256dh);
					break;
				case Constants.ID_CRED_TYPE_X5CHAIN:
					idCredP256dh = Util.buildIdCredX5chain(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5T:
					idCredP256dh = Util.buildIdCredX5t(serializedCert);
					break;
				case Constants.ID_CRED_TYPE_X5U:
					idCredP256dh = Util.buildIdCredX5u("http://example.repo.com/hostA-x509-P256-dh");
					break;
			}

			// Add the key pair, CRED and ID_CRED to the respective collections
			keyPairs.get(Constants.ECDH_KEY).
					put(Constants.CURVE_P256, keyPairP256dh);
			creds.get(Constants.ECDH_KEY).
					put(Constants.CURVE_P256, CBORObject.FromObject(credP256dh));
			idCreds.get(Constants.ECDH_KEY).
					put(Constants.CURVE_P256, idCredP256dh);

			// Add this ID_CRED to the whole collection of ID_CRED_X for this peer
			ownIdCreds.add(idCredP256dh);

		}

	}

	private static void setupPeerAuthenticationCredentials () {

		byte[] peerPublicKeyBinary;
		byte[] peerPublicKeyBinaryY;
		byte[] peerCred;
		byte[] peerSerializedCert;

		// The subject name used for the identity key of the other peer
		String peerSubjectName = "";


		/* *** *** *** *** */
		//
		// Add other peers' authentication credentials for curve Ed25519
		//
		/* *** *** *** *** */

		OneKey peer1PublicKeyEd25519;
		CBORObject peer1CcsObjectEd25519;
		CBORObject peer1IdCredEd25519kccs;
		CBORObject peer1IdCredEd25519kid;
		CBORObject peer1IdCredEd25519x5chain;
		CBORObject peer1IdCredEd25519x5t;
		CBORObject peer1IdCredEd25519x5u;

		// If the type of credential identifier is 'kid', use 0x07,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x07
		byte[] peer1KidEd25519 = new byte[] {(byte) 0x07};


		// Build the public key

		peerPublicKeyBinary = hexToBytes("dbd9dc8cd03fb7c3913511462bb23816477c6bd8d66ef5a1a070ac854ed73fd2");
		peer1PublicKeyEd25519 =  SharedSecretCalculation.buildEd25519OneKey(null, peerPublicKeyBinary);


		// Build CRED as a CCS, and the corresponding ID_CRED as 'kccs' and 'kid'

		System.out.print("Peer ");
		CBORObject peer1KidCborEd25519 = CBORObject.FromObject(peer1KidEd25519);
		peer1CcsObjectEd25519 = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(peer1PublicKeyEd25519,
				peerSubjectName, peer1KidCborEd25519));

		// These serializations have to be prepared manually, in order to ensure that
		// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
		peerCred = hexToBytes("a2026008a101a401010241072006215820dbd9dc8cd03fb7c3913511462bb23816477c6bd8d66e"
				+ "f5a1a070ac854ed73fd2");

		peer1IdCredEd25519kccs = Util.buildIdCredKccs(peer1CcsObjectEd25519); // ID_CRED as 'kccs'
		peer1IdCredEd25519kid = Util.buildIdCredKid(peer1KidEd25519); // ID_CRED as 'kid'

		peerPublicKeys.put(peer1IdCredEd25519kccs, peer1PublicKeyEd25519);
		peerCredentials.put(peer1IdCredEd25519kccs, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredEd25519kid, peer1PublicKeyEd25519);
		peerCredentials.put(peer1IdCredEd25519kid, CBORObject.FromObject(peerCred));


		// Build CRED as an X.509 certificate, and the corresponding ID_CRED as 'x5chain', 'x5t' and 'x5u'
		peerSerializedCert = hexToBytes("c788370016b8965bdb2074bff82e5a20e09bec21f8406e86442b87ec3ff245b70a47624dc"
				+ "9cdc6824b2a4c52e95ec9d6b0534b71c2b49e4bf9031500cee6869979c297bb5a8b381e98db714108415e5c50db78974c2"
				+ "71579b01633a3ef6271be5c225eb2");

		// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
		peerCred = CBORObject.FromObject(peerSerializedCert).EncodeToBytes();

		peer1IdCredEd25519x5chain = Util.buildIdCredX5chain(peerSerializedCert); // ID_CRED as 'x5chain'
		peer1IdCredEd25519x5t = Util.buildIdCredX5t(peerSerializedCert); // ID_CRED as 'x5t'
		peer1IdCredEd25519x5u = Util.buildIdCredX5u("http://example.repo.com/hostB-x509-Ed25519"); // ID_CRED as 'x5u'

		peerPublicKeys.put(peer1IdCredEd25519x5chain, peer1PublicKeyEd25519);
		peerCredentials.put(peer1IdCredEd25519x5chain, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredEd25519x5t, peer1PublicKeyEd25519);
		peerCredentials.put(peer1IdCredEd25519x5t, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredEd25519x5u, peer1PublicKeyEd25519);
		peerCredentials.put(peer1IdCredEd25519x5u, CBORObject.FromObject(peerCred));


		/* *** *** *** *** */
		//
		// Add other peers' authentication credentials for curve X25519
		//
		/* *** *** *** *** */

		OneKey peer1PublicKeyX25519;
		CBORObject peer1CcsObjectX25519;
		CBORObject peer1IdCredX25519kccs;
		CBORObject peer1IdCredX25519kid;
		CBORObject peer1IdCredX25519x5chain;
		CBORObject peer1IdCredX25519x5t;
		CBORObject peer1IdCredX25519x5u;

		// If the type of credential identifier is 'kid', use 0x08,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x08
		byte[] peer1KidX25519 = new byte[] {(byte) 0x08};

		// Build the public key

		peerPublicKeyBinary = hexToBytes("a3ff263595beb377d1a0ce1d04dad2d40966ac6bcb622051b84659184d5d9a32");
		peer1PublicKeyX25519 =  SharedSecretCalculation.buildCurve25519OneKey(null, peerPublicKeyBinary);


		// Build CRED as a CCS, and the corresponding ID_CRED as 'kccs' and 'kid'

		System.out.print("Peer ");
		CBORObject peer1KidCborX25519 = CBORObject.FromObject(peer1KidX25519);
		peer1CcsObjectX25519 = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(peer1PublicKeyX25519,
				peerSubjectName, peer1KidCborX25519));

		// These serializations have to be prepared manually, in order to ensure that
		// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
		peerCred = hexToBytes("A2026008A101A401010241082004215820A3FF263595BEB377D1A0CE1D04DAD2D40966AC6BCB622051B"
				+ "84659184D5D9A32");

		peer1IdCredX25519kccs = Util.buildIdCredKccs(peer1CcsObjectX25519); // ID_CRED as 'kccs'
		peer1IdCredX25519kid = Util.buildIdCredKid(peer1KidX25519); // ID_CRED as 'kid'

		peerPublicKeys.put(peer1IdCredX25519kccs, peer1PublicKeyX25519);
		peerCredentials.put(peer1IdCredX25519kccs, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredX25519kid, peer1PublicKeyX25519);
		peerCredentials.put(peer1IdCredX25519kid, CBORObject.FromObject(peerCred));


		// Build CRED as an X.509 certificate, and the corresponding ID_CRED as 'x5chain', 'x5t' and 'x5u'
		peerSerializedCert = hexToBytes("c788370016b8965bdb2074bff82e5a20e09bec21f8406e86442b87ec3ff245b70a47"
				+ "624dc9cdc6824b2a4c52e95ec9d6b0534b71c2b49e4bf9031500cee6869979c297bb5a8b381e98db714108415e5c5"
				+ "0db78974c271579b01633a3ef6271be5c225eb3");

		// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
		peerCred = CBORObject.FromObject(peerSerializedCert).EncodeToBytes();

		peer1IdCredX25519x5chain = Util.buildIdCredX5chain(peerSerializedCert); // ID_CRED as 'x5chain'
		peer1IdCredX25519x5t = Util.buildIdCredX5t(peerSerializedCert); // ID_CRED as 'x5t'
		peer1IdCredX25519x5u = Util.buildIdCredX5u("http://example.repo.com/hostB-x509-X25519"); // ID_CRED as 'x5u'

		peerPublicKeys.put(peer1IdCredX25519x5chain, peer1PublicKeyX25519);
		peerCredentials.put(peer1IdCredX25519x5chain, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredX25519x5t, peer1PublicKeyX25519);
		peerCredentials.put(peer1IdCredX25519x5t, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredX25519x5u, peer1PublicKeyX25519);
		peerCredentials.put(peer1IdCredX25519x5u, CBORObject.FromObject(peerCred));


		/* *** *** *** *** */
		//
		// Add other peers' authentication credentials for curve P-256 (one for signing only, one for ECDH only)
		//
		/* *** *** *** *** */

		// Signing authentication credential

		OneKey peer1PublicKeyP256;
		CBORObject peer1CcsObjectP256;
		CBORObject peer1IdCredP256kccs;
		CBORObject peer1IdCredP256kid;
		CBORObject peer1IdCredP256x5chain;
		CBORObject peer1IdCredP256x5t;
		CBORObject peer1IdCredP256x5u;

		// If the type of credential identifier is 'kid', use 0x09,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x09
		byte[] peer1KidP256 = new byte[] {(byte) 0x09};

		// Build the public key

		peerPublicKeyBinary = hexToBytes("6f9702a66602d78f5e81bac1e0af01f8b52810c502e87ebb7c926c07426fd02f");
		peerPublicKeyBinaryY = hexToBytes("C8D33274C71C9B3EE57D842BBF2238B8283CB410ECA216FB72A78EA7A870F800");
		peer1PublicKeyP256 =  SharedSecretCalculation.buildEcdsa256OneKey(null, peerPublicKeyBinary,
				peerPublicKeyBinaryY);


		// Build CRED as a CCS, and the corresponding ID_CRED as 'kccs' and 'kid'

		System.out.print("Peer ");
		CBORObject peer1KidCborP256 = CBORObject.FromObject(peer1KidP256);
		peer1CcsObjectP256 = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(peer1PublicKeyP256,
				peerSubjectName, peer1KidCborP256));

		// These serializations have to be prepared manually, in order to ensure that
		// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
		peerCred = hexToBytes("A2026008A101A5010202410920012158206F9702A66602D78F5E81BAC1E0AF01F8B52810C502E"
				+ "87EBB7C926C07426FD02F225820C8D33274C71C9B3EE57D842BBF2238B8283CB410ECA216FB72A78EA7A870F800");

		peer1IdCredP256kccs = Util.buildIdCredKccs(peer1CcsObjectP256); // ID_CRED as 'kccs'
		peer1IdCredP256kid = Util.buildIdCredKid(peer1KidP256); // ID_CRED as 'kid'

		peerPublicKeys.put(peer1IdCredP256kccs, peer1PublicKeyP256);
		peerCredentials.put(peer1IdCredP256kccs, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredP256kid, peer1PublicKeyP256);
		peerCredentials.put(peer1IdCredP256kid, CBORObject.FromObject(peerCred));


		// Build CRED as an X.509 certificate, and the corresponding ID_CRED as 'x5chain', 'x5t' and 'x5u'
		peerSerializedCert = hexToBytes("c788370016b8965bdb2074bff82e5a20e09bec21f8406e86442b87ec3ff245b70a47624d"
				+ "c9cdc6824b2a4c52e95ec9d6b0534b71c2b49e4bf9031500cee6869979c297bb5a8b381e98db714108415e5c50db78974"
				+ "c271579b01633a3ef6271be5c225eb4");

		// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
		peerCred = CBORObject.FromObject(peerSerializedCert).EncodeToBytes();

		peer1IdCredP256x5chain = Util.buildIdCredX5chain(peerSerializedCert); // ID_CRED as 'x5chain'
		peer1IdCredP256x5t = Util.buildIdCredX5t(peerSerializedCert); // ID_CRED as 'x5t'
		peer1IdCredP256x5u = Util.buildIdCredX5u("http://example.repo.com/hostB-x509-P256-signing"); // ID_CRED as 'x5u'

		peerPublicKeys.put(peer1IdCredP256x5chain, peer1PublicKeyP256);
		peerCredentials.put(peer1IdCredP256x5chain, CBORObject.FromObject(peerCred));

		peerPublicKeys.put(peer1IdCredP256x5t, peer1PublicKeyP256);
		peerCredentials.put(peer1IdCredP256x5t, CBORObject.FromObject(peerCred));

		peerPublicKeys.put(peer1IdCredP256x5u, peer1PublicKeyP256);
		peerCredentials.put(peer1IdCredP256x5u, CBORObject.FromObject(peerCred));

		// ECDH authentication credential

		OneKey peer1PublicKeyP256DH;
		CBORObject peer1CcsObjectP256DH;
		CBORObject peer1IdCredP256DHkccs;
		CBORObject peer1IdCredP256DHkid;
		CBORObject peer1IdCredP256DHx5chain;
		CBORObject peer1IdCredP256DHx5t;
		CBORObject peer1IdCredP256DHx5u;

		// If the type of credential identifier is 'kid', use 0x0a,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x0a
		byte[] peer1KidP256DH = new byte[] {(byte) 0x0a};

		// Build the public key

		peerPublicKeyBinary = hexToBytes("bbc34960526ea4d32e940cad2a234148ddc21791a12afbcbac93622046dd44f0");
		peerPublicKeyBinaryY = hexToBytes("4519e257236b2a0ce2023f0931f1f386ca7afda64fcde0108c224c51eabf6072");
		peer1PublicKeyP256DH =  SharedSecretCalculation.buildEcdsa256OneKey(null, peerPublicKeyBinary,
				peerPublicKeyBinaryY);


		// Build CRED as a CCS, and the corresponding ID_CRED as 'kccs' and 'kid'

		System.out.print("Peer ");
		CBORObject peer1KidCborP256DH = CBORObject.FromObject(peer1KidP256DH);
		peer1CcsObjectP256DH = CBORObject.DecodeFromBytes(Util.buildCredRawPublicKeyCcs(
				peer1PublicKeyP256DH, peerSubjectName, peer1KidCborP256DH));

		// These serializations have to be prepared manually, in order to ensure that
		// the CBOR map used as CRED has its parameters encoded in bytewise lexicographic order
		peerCred = hexToBytes("A2026008A101A5010202410A2001215820BBC34960526EA4D32E940CAD2A234148DDC21791A12AFBC"
				+ "BAC93622046DD44F02258204519E257236B2A0CE2023F0931F1F386CA7AFDA64FCDE0108C224C51EABF6072");

		peer1IdCredP256DHkccs = Util.buildIdCredKccs(peer1CcsObjectP256DH); // ID_CRED as 'kccs'
		peer1IdCredP256DHkid = Util.buildIdCredKid(peer1KidP256DH); // ID_CRED as 'kid'

		peerPublicKeys.put(peer1IdCredP256DHkccs, peer1PublicKeyP256DH);
		peerCredentials.put(peer1IdCredP256DHkccs, CBORObject.FromObject(peerCred));
		peerPublicKeys.put(peer1IdCredP256DHkid, peer1PublicKeyP256DH);
		peerCredentials.put(peer1IdCredP256DHkid, CBORObject.FromObject(peerCred));


		// Build CRED as an X.509 certificate, and the corresponding ID_CRED as 'x5chain', 'x5t' and 'x5u'
		peerSerializedCert = hexToBytes("4488370016b8965bdb2074bff82e5a20e09bec21f8406e86442b87ec3ff245b70a47624dc9c"
				+ "dc6824b2a4c52e95ec9d6b0534b71c2b49e4bf9031500cee6869979c297bb5a8b381e98db714108415e5c50db78974c27157"
				+ "9b01633a3ef6271be5c225eb4");

		// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
		peerCred = CBORObject.FromObject(peerSerializedCert).EncodeToBytes();

		peer1IdCredP256DHx5chain = Util.buildIdCredX5chain(peerSerializedCert); // ID_CRED as 'x5chain'
		peer1IdCredP256DHx5t = Util.buildIdCredX5t(peerSerializedCert); // ID_CRED as 'x5t'
		peer1IdCredP256DHx5u = Util.buildIdCredX5u("http://example.repo.com/hostB-x509-P256-dh"); // ID_CRED as 'x5u'

		peerPublicKeys.put(peer1IdCredP256DHx5chain, peer1PublicKeyP256DH);
		peerCredentials.put(peer1IdCredP256DHx5chain, CBORObject.FromObject(peerCred));

		peerPublicKeys.put(peer1IdCredP256DHx5t, peer1PublicKeyP256DH);
		peerCredentials.put(peer1IdCredP256DHx5t, CBORObject.FromObject(peerCred));

		peerPublicKeys.put(peer1IdCredP256DHx5u, peer1PublicKeyP256DH);
		peerCredentials.put(peer1IdCredP256DHx5u, CBORObject.FromObject(peerCred));

	}
}
