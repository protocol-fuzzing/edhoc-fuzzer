package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.AuthenticationConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.keyConfigs.KeyConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.ClientMapperConnector;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.BigIntegers;
import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.*;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/** Adapted from test file EdhocClient from edhoc repo */
public class ClientMapperState extends EdhocMapperState {

	protected Provider EdDSA = new EdDSASecurityProvider();
	protected Provider BouncyCastle = new BouncyCastleProvider();

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
	protected HashMap<CBORObject, EdhocSession> edhocSessions = new HashMap<>();

	// Each element is a used Connection Identifier offered to the other peers.
	// Connection Identifiers are stored as CBOR integers (if numeric) or as CBOR byte strings (if binary)
	protected Set<CBORObject> usedConnectionIds = new HashSet<>();

	// List of supported cipher suites, in decreasing order of preference.
	protected List<Integer> supportedCipherSuites = new ArrayList<>();

	// The collection of application profiles - The lookup key is the full URI of the EDHOC resource
	protected HashMap<String, AppProfile> appProfiles = new HashMap<>();

	// The shared database of OSCORE Security Contexts
	// It needs to be static in order to be the same as the one in
	// edhoc stack layers initialized by EdhocCoapStackFactory.useAsDefault
	protected HashMapCtxDB db = new HashMapCtxDB();

	// The size of the Replay Window to use in an OSCORE Recipient Context
	protected int OSCORE_REPLAY_WINDOW = 32;

	// The size to consider for MAX_UNFRAGMENTED SIZE
	protected int MAX_UNFRAGMENTED_SIZE = 4096;

	protected EdhocSession edhocSession;
	protected EdhocEndpointInfo edhocEndpointInfo;

	protected String edhocURI;

	public ClientMapperState(String edhocURI, int appProfileMode, AuthenticationConfig authenticationConfig,
							 ClientMapperConnector clientMapperConnector) {
		// edhocURI should be coap://localhost:port/.well-known/edhoc
		this.edhocURI = edhocURI;

		// Insert security providers
		Security.insertProviderAt(EdDSA, 1);
		Security.insertProviderAt(BouncyCastle, 2);

		// Set authentication params
		this.authenticationMethod = authenticationConfig.getMapAuthenticationMethod();
		this.credType = authenticationConfig.getMapCredType();
		this.idCredType = authenticationConfig.getMapIdCredType();

		// Add the supported cipher suites in decreasing order of preference
		this.supportedCipherSuites.addAll(authenticationConfig.getMapSupportedCipherSuites());

		// Set up the authentication credentials for this peer
		setupOwnAuthenticationCredentials(authenticationConfig);

		// Set up the authentication credentials for the other peers
		setupPeerAuthenticationCredentials(authenticationConfig);

		// Set the application profile

		// Supported authentication methods
		Set<Integer> authMethods = new HashSet<>();
		for (int i = 0; i <= Constants.EDHOC_AUTH_METHOD_3; i++) {
			authMethods.add(i);
		}

		// Use of message_4 as expected to be sent by the Responder
		boolean useMessage4;

		// Use of EDHOC for keying OSCORE
		boolean usedForOSCORE;

		// Supporting for the EDHOC+OSCORE request
		boolean supportCombinedRequest;

		switch (appProfileMode) {
			case 1 -> {
				// m3 no app
				useMessage4 = false;
				usedForOSCORE = false;
				supportCombinedRequest = false;
			}
			case 2 -> {
				// m3 app
				useMessage4 = false;
				usedForOSCORE = true;
				supportCombinedRequest = false;
			}
			case 3 -> {
				// m3 combined app
				useMessage4 = false;
				usedForOSCORE = true;
				supportCombinedRequest = true;
			}
			case 4 -> {
				// m4 no app
				useMessage4 = true;
				usedForOSCORE = false;
				supportCombinedRequest = false;
			}
			case 5 -> {
				// m4 app
				useMessage4 = true;
				usedForOSCORE = true;
				supportCombinedRequest = false;
			}
			default -> throw new RuntimeException("Invalid application profile mode: " + appProfileMode
					+ ". Available application profile modes are 1, 2, 3, 4, 5");
		}

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
		HashMapCtxDB oscoreDB = appProfile.getUsedForOSCORE() ? db : null;
		byte[] connectionId = Util.getConnectionId(usedConnectionIds, oscoreDB, null);
		edhocSession = new EdhocSessionPersistent(
				true, true, authenticationMethod, connectionId,
				keyPairs, idCreds, creds, supportedCipherSuites, appProfile, edp, oscoreDB);

		// Update edhocSessions
		edhocSessions.put(CBORObject.FromObject(connectionId), edhocSession);

		// Create a dummy oscore context if needed
		if (appProfile.getUsedForOSCORE()) {
			setupOscoreContext();
		}

		// Create new clients
		clientMapperConnector.createNewClients(new EdhocStackFactoryPersistent(edhocEndpointInfo,
				new MessageProcessorPersistent(this)));
	}

	@Override
	public EdhocSession getEdhocSession() {
		return edhocSession;
	}
	@Override
	public EdhocEndpointInfo getEdhocEndpointInfo() {
		return edhocEndpointInfo;
	}

	@Override
	public Set<CBORObject> getOwnIdCreds() {
		return ownIdCreds;
	}

	@Override
	public void setupOscoreContext() {
		/* Invoke the EDHOC-Exporter to produce OSCORE input material */
		byte[] masterSecret = EdhocSession.getMasterSecretOSCORE(edhocSession);
		byte[] masterSalt = EdhocSession.getMasterSaltOSCORE(edhocSession);

		/* Set up the OSCORE Security Context */

		// The Sender ID of this peer is the EDHOC connection identifier of the other peer
		byte[] senderId = edhocSession.getPeerConnectionId();
		// The Recipient ID of this peer is the EDHOC connection identifier of this peer
		byte[] recipientId = edhocSession.getConnectionId();

		int selectedCipherSuite = edhocSession.getSelectedCipherSuite();
		AlgorithmID alg = EdhocSession.getAppAEAD(selectedCipherSuite);
		AlgorithmID hkdf = EdhocSession.getAppHkdf(selectedCipherSuite);

		OSCoreCtx ctx;
		if (Arrays.equals(senderId, recipientId)) {
			throw new RuntimeException("Error: the Sender ID coincides with the Recipient ID");
		}

		try {
			ctx = new OSCoreCtx(masterSecret, true, alg, senderId, recipientId, hkdf,
					OSCORE_REPLAY_WINDOW, masterSalt, null, MAX_UNFRAGMENTED_SIZE);
		} catch (OSException e) {
			throw new RuntimeException("Error when deriving the OSCORE Security Context: " + e.getMessage());
		}

		try {
			db.addContext(edhocURI, ctx);
		} catch (OSException e) {
			throw new RuntimeException("Error when adding the OSCORE Security Context to the context database: "
					+ e.getMessage());
		}
	}

	protected void setupOwnAuthenticationCredentials (AuthenticationConfig authenticationConfig) {
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
		KeyConfig keyConfig;
		byte[] privateKey, publicKey;
		OneKey keyPair;

		if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_0)
				|| supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_1)) {

			switch (authenticationMethod) {
				case Constants.EDHOC_AUTH_METHOD_0, Constants.EDHOC_AUTH_METHOD_1 -> {
					// Curve Ed25519 (SIG)
					keyConfig = authenticationConfig.getEd25519KeySigConfig();
					privateKey = readEd25519PrivateDerFile(keyConfig.getMapPrivateFilename());
					publicKey = readEd25519PublicDerFile(keyConfig.getMapPublicFilename());

					// Build key pair
					keyPair = SharedSecretCalculation.buildEd25519OneKey(privateKey, publicKey);

					// Add the credentials
					addOwnCredentials(credType, idCredType, keyPair, keyConfig,
							Constants.SIGNATURE_KEY, Constants.CURVE_Ed25519, subjectName);
				}
				case Constants.EDHOC_AUTH_METHOD_2,  Constants.EDHOC_AUTH_METHOD_3 -> {
					// Curve X25519 (STAT)
					keyConfig = authenticationConfig.getX25519KeyStatConfig();
					privateKey = readX25519PrivateDerFile(keyConfig.getMapPrivateFilename());
					publicKey = readX25519PublicDerFile(keyConfig.getMapPublicFilename());

					// Build key pair
					keyPair = SharedSecretCalculation.buildCurve25519OneKey(privateKey, publicKey);

					// Add the credentials
					addOwnCredentials(credType, idCredType, keyPair, keyConfig,
							Constants.ECDH_KEY, Constants.CURVE_X25519, subjectName);
				}
				default -> throw new RuntimeException("Invalid authentication method: " + authenticationMethod);
			}
		}

		if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_2) ||
				supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_3)) {
			byte[] publicKeyX, publicKeyY;

			switch (authenticationMethod) {
				case Constants.EDHOC_AUTH_METHOD_0, Constants.EDHOC_AUTH_METHOD_1 -> {
					// P-256 (SIG)
					keyConfig = authenticationConfig.getP256KeySigConfig();
					privateKey = readP256PrivateDerFile(keyConfig.getMapPrivateFilename());
					publicKeyX = new byte[32];
					publicKeyY = new byte[32];
					readP256PublicDerFile(keyConfig.getMapPublicFilename(), publicKeyX, publicKeyY);

					// Build the key pair
					keyPair = SharedSecretCalculation.buildEcdsa256OneKey(privateKey, publicKeyX, publicKeyY);

					// Add the credentials
					addOwnCredentials(credType, idCredType, keyPair, keyConfig,
							Constants.SIGNATURE_KEY, Constants.CURVE_P256, subjectName);
				}
				case Constants.EDHOC_AUTH_METHOD_2, Constants.EDHOC_AUTH_METHOD_3 -> {
					// P-256 (STAT)
					keyConfig = authenticationConfig.getP256KeyStatConfig();
					privateKey = readP256PrivateDerFile(keyConfig.getMapPrivateFilename());
					publicKeyX = new byte[32];
					publicKeyY = new byte[32];
					readP256PublicDerFile(keyConfig.getMapPublicFilename(), publicKeyX, publicKeyY);

					// Build the key pair
					keyPair = SharedSecretCalculation.buildEcdsa256OneKey(privateKey, publicKeyX, publicKeyY);

					// Add the credentials
					addOwnCredentials(credType, idCredType, keyPair, keyConfig,
							Constants.ECDH_KEY, Constants.CURVE_P256, subjectName);
				}
				default -> throw new RuntimeException("Invalid authentication method: " + authenticationMethod);
			}
		}
	}

	protected void setupPeerAuthenticationCredentials (AuthenticationConfig authenticationConfig) {
		// Add as many authentication credentials are provided

		// The subject name used for the identity key of the other peer
		Integer credType = authenticationConfig.getSulCredType();
		Integer idCredType = authenticationConfig.getSulIdCredType();
		String subjectName = "";
		String publicKeyFilename;
		KeyConfig keyConfig;
		OneKey keyPair;
		byte[] publicKey, publicKeyX, publicKeyY;

		switch (authenticationMethod) {
			case Constants.EDHOC_AUTH_METHOD_0, Constants.EDHOC_AUTH_METHOD_2 -> {
				// Add other peers' authentication credentials for curve Ed25519 (SIG)
				keyConfig = authenticationConfig.getEd25519KeySigConfig();
				publicKeyFilename = keyConfig.getSulPublicFilename();
				if (publicKeyFilename != null) {
					publicKey = readEd25519PublicDerFile(publicKeyFilename);

					// Build the keyPair only from public key
					keyPair = SharedSecretCalculation.buildEd25519OneKey(null, publicKey);

					// Add the credentials
					addPeerCredentials(credType, idCredType, keyPair, keyConfig, subjectName);
				}

				// Add other peers' authentication credentials for curve P-256 (SIG)
				keyConfig = authenticationConfig.getP256KeySigConfig();
				publicKeyFilename = keyConfig.getSulPublicFilename();
				if (publicKeyFilename != null) {
					publicKeyX = new byte[32];
					publicKeyY = new byte[32];
					readP256PublicDerFile(publicKeyFilename, publicKeyX, publicKeyY);

					// Build the keyPair from only the public key
					keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, publicKeyX, publicKeyY);

					// Add the credentials
					addPeerCredentials(credType, idCredType, keyPair, keyConfig, subjectName);
				}
			}
			case Constants.EDHOC_AUTH_METHOD_1, Constants.EDHOC_AUTH_METHOD_3 -> {
				// Add other peers' authentication credentials for curve X25519 (STAT)
				keyConfig = authenticationConfig.getX25519KeyStatConfig();
				publicKeyFilename = keyConfig.getSulPublicFilename();
				if (publicKeyFilename != null) {
					publicKey = readX25519PublicDerFile(publicKeyFilename);

					// Build the keyPair only from public key
					keyPair = SharedSecretCalculation.buildCurve25519OneKey(null, publicKey);

					// Add the credentials
					addPeerCredentials(credType, idCredType, keyPair, keyConfig, subjectName);
				}

				// Add other peers' authentication credentials for curve P-256 (STAT)
				keyConfig = authenticationConfig.getP256KeyStatConfig();
				publicKeyFilename = keyConfig.getSulPublicFilename();
				if (publicKeyFilename != null) {
					publicKeyX = new byte[32];
					publicKeyY = new byte[32];
					readP256PublicDerFile(publicKeyFilename, publicKeyX, publicKeyY);

					// Build the keyPair from only the public key
					keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, publicKeyX, publicKeyY);

					// Add the credentials
					addPeerCredentials(credType, idCredType, keyPair, keyConfig, subjectName);
				}
			}
		}
	}

	protected byte[] derFileToBytes(String filename) {
		if (filename == null) {
			throw new RuntimeException("Null DER filename provided");
		}

		try {
			return Files.readAllBytes(Paths.get(filename));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected byte[] readEd25519PrivateDerFile(String filename) {
		try {
			byte[] data = derFileToBytes(filename);
			return ((Ed25519PrivateKeyParameters) PrivateKeyFactory.createKey(data)).getEncoded();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected byte[] readEd25519PublicDerFile(String filename) {
		try {
			byte[] data = derFileToBytes(filename);
			return ((Ed25519PublicKeyParameters) PublicKeyFactory.createKey(data)).getEncoded();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected byte[] readX25519PrivateDerFile(String filename) {
		try {
			byte[] data = derFileToBytes(filename);
			return ((X25519PrivateKeyParameters) PrivateKeyFactory.createKey(data)).getEncoded();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected byte[] readX25519PublicDerFile(String filename) {
		try {
			byte[] data = derFileToBytes(filename);
			return ((X25519PublicKeyParameters) PublicKeyFactory.createKey(data)).getEncoded();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected byte[] readP256PrivateDerFile(String filename) {
		try {
			byte[] data = derFileToBytes(filename);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(data);
			KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");
			ECPrivateKey privateKey = (ECPrivateKey) keyFactory.generatePrivate(spec);
			return BigIntegers.asUnsignedByteArray(privateKey.getD());
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	protected void readP256PublicDerFile(String filename, byte[] publicX, byte[] publicY) {
		try {
			byte[] data = derFileToBytes(filename);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
			KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");
			ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(spec);
			byte[] x = publicKey.getQ().getAffineXCoord().getEncoded();
			System.arraycopy(x, 0, publicX, 0, publicX.length);
			byte[] y = publicKey.getQ().getAffineYCoord().getEncoded();
			System.arraycopy(y, 0, publicY, 0, publicY.length);
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	protected byte[] buildCred(int credType, byte[] kid, OneKey keyPair, String subjectName, byte[] serializedCert) {
		switch (credType) {
			case Constants.CRED_TYPE_CWT -> throw new UnsupportedOperationException("Cred type: CWT");
			case Constants.CRED_TYPE_CCS -> {
				String error = "";
				error += kid == null ? " kid" : "";
				error += keyPair == null ? " keyPair" : "";
				error += subjectName == null ? " subject name" : "";

				if (!error.equals("")) {
					throw new RuntimeException("Null provided:" + error);
				}

				CBORObject idCredKidCbor = CBORObject.FromObject(kid);
				return Util.buildCredRawPublicKeyCcs(keyPair, subjectName, idCredKidCbor);
			}
			case Constants.CRED_TYPE_X509 -> {
				if (serializedCert == null) {
					throw new RuntimeException("Null provided serialized certificate");
				}
				// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
				return CBORObject.FromObject(serializedCert).EncodeToBytes();
			}
			default -> throw new IllegalStateException("Unexpected credType value: " + credType);
		}
	}

	protected CBORObject buildIdCred(int idCredType, byte[] kid, byte[] cred, byte[] serializedCert, String x5uLink) {
		switch (idCredType) {
			case Constants.ID_CRED_TYPE_KID -> {
				if (kid == null) {
					throw new RuntimeException("Null provided kid");
				}
				return Util.buildIdCredKid(kid);
			}
			case Constants.ID_CRED_TYPE_CWT -> throw new UnsupportedOperationException("Id Cred type: CWT ");
			case Constants.ID_CRED_TYPE_CCS -> {
				if (cred == null) {
					throw new RuntimeException("Null provided cred");
				}
				return Util.buildIdCredKccs(CBORObject.DecodeFromBytes(cred));
			}
			case Constants.ID_CRED_TYPE_X5T -> {
				if (serializedCert == null) {
					throw new RuntimeException("Null provided serialized certificate");
				}
				return Util.buildIdCredX5t(serializedCert);
			}
			case Constants.ID_CRED_TYPE_X5U -> {
				if (x5uLink == null) {
					throw new RuntimeException("Null provided x5uLink");
				}
				return Util.buildIdCredX5u(x5uLink);
			}
			case Constants.ID_CRED_TYPE_X5CHAIN -> {
				if (serializedCert == null) {
					throw new RuntimeException("Null provided serialized certificate");
				}
				return Util.buildIdCredX5chain(serializedCert);
			}
			default -> throw new IllegalStateException("Unexpected idCredType value: " + idCredType);
		}
	}

	protected void addOwnCredentials(int credType, int idCredType, OneKey keyPair, KeyConfig keyConfig,
									 int keyUsage, int keyCurve, String subjectName){
		byte[] kid = keyConfig.getMapKid();
		String certFilename = keyConfig.getMapX509Filename();
		byte[] serializedCert = certFilename == null ? null : derFileToBytes(certFilename);
		String x5uLink = keyConfig.getMapX5uLink();

		// Build CRED
		byte[] cred = buildCred(credType, kid, keyPair, subjectName, serializedCert);

		// Build ID_CRED
		CBORObject idCred = buildIdCred(idCredType, kid, cred, serializedCert, x5uLink);

		// Add the key pair, CRED and ID_CRED to the respective collections
		keyPairs.get(keyUsage).put(keyCurve, keyPair);
		creds.get(keyUsage).put(keyCurve, CBORObject.FromObject(cred));
		idCreds.get(keyUsage).put(keyCurve, idCred);

		// Add this ID_CRED to the whole collection of ID_CRED_X for this peer
		ownIdCreds.add(idCred);
	}

	protected void addAllPeerIdCredForCCSCred(byte[] cred, OneKey peerPublicKey, byte[] kid) {
		// ID_CRED as 'kccs'
		CBORObject idCredkccs = buildIdCred(Constants.ID_CRED_TYPE_CCS, null, cred, null, null);
		peerPublicKeys.put(idCredkccs, peerPublicKey);
		peerCredentials.put(idCredkccs, CBORObject.FromObject(cred));

		// ID_CRED as 'kid'
		CBORObject idCredkid = buildIdCred(Constants.ID_CRED_TYPE_KID, kid, null, null, null);
		peerPublicKeys.put(idCredkid, peerPublicKey);
		peerCredentials.put(idCredkid, CBORObject.FromObject(cred));
	}

	protected void addAllPeerIdCredForX509Cred(byte[] cred, OneKey peerPublicKey, byte[] serializedCert,
											   String x5uLink) {
		// ID_CRED as 'x5t'
		CBORObject idCredx5t = buildIdCred(Constants.ID_CRED_TYPE_X5T, null, null, serializedCert, null);
		peerPublicKeys.put(idCredx5t, peerPublicKey);
		peerCredentials.put(idCredx5t, CBORObject.FromObject(cred));

		// ID_CRED as 'x5chain'
		CBORObject idCredx5chain = buildIdCred(Constants.ID_CRED_TYPE_X5CHAIN, null, null, serializedCert, null);
		peerPublicKeys.put(idCredx5chain, peerPublicKey);
		peerCredentials.put(idCredx5chain, CBORObject.FromObject(cred));

		// ID_CRED as 'x5u'
		if (x5uLink != null) {
			CBORObject idCredx5u = buildIdCred(Constants.ID_CRED_TYPE_X5U, null, null, null, x5uLink);
			peerPublicKeys.put(idCredx5u, peerPublicKey);
			peerCredentials.put(idCredx5u, CBORObject.FromObject(cred));
		}
	}

	protected void addPeerCredentials(Integer credType, Integer idCredType, OneKey peerPublicKey,
									  KeyConfig keyConfig, String subjectName) {

		byte[] kid = keyConfig.getSulKid();
		String certFilename = keyConfig.getSulX509Filename();
		byte[] serializedCert = certFilename == null ? null : derFileToBytes(certFilename);
		String x5uLink = keyConfig.getSulX5uLink();
		byte[] cred;
		CBORObject idCred;

		// Nothing is known for the peer yet, so add as many id_creds and creds as possible

		if (credType != null) {
			cred = buildCred(credType, kid, peerPublicKey, subjectName, serializedCert);

			if (idCredType != null) {
				idCred = buildIdCred(idCredType, kid, cred, serializedCert, x5uLink);
				peerPublicKeys.put(idCred, peerPublicKey);
				peerCredentials.put(idCred, CBORObject.FromObject(cred));
			} else {
				switch (credType) {
					case Constants.CRED_TYPE_CCS ->
							addAllPeerIdCredForCCSCred(cred, peerPublicKey, kid);
					case Constants.CRED_TYPE_X509 ->
							addAllPeerIdCredForX509Cred(cred, peerPublicKey, serializedCert, x5uLink);
				}
			}
		}
		else {
			// Build CRED as a CCS
			byte[] cred_ccs = buildCred(Constants.CRED_TYPE_CCS, kid, peerPublicKey, subjectName, null);
			// Build CRED as an X.509 certificate
			byte[] cred_x509 = buildCred(Constants.CRED_TYPE_X509, null, null, null, serializedCert);

			if (idCredType != null) {
				if (idCredType == Constants.ID_CRED_TYPE_KID || idCredType == Constants.ID_CRED_TYPE_CCS) {
						cred = cred_ccs;
						idCred = buildIdCred(idCredType, kid, cred_ccs, null, null);
				}
				else if	(idCredType == Constants.ID_CRED_TYPE_X5T || idCredType == Constants.ID_CRED_TYPE_X5U
						|| idCredType == Constants.ID_CRED_TYPE_X5CHAIN) {
					cred = cred_x509;
					idCred = buildIdCred(idCredType, null, cred_x509, serializedCert, x5uLink);
				}
				else {
					throw new UnsupportedOperationException("Id cred type: " + idCredType);
				}
				peerPublicKeys.put(idCred, peerPublicKey);
				peerCredentials.put(idCred, CBORObject.FromObject(cred));
			} else {
				addAllPeerIdCredForCCSCred(cred_ccs, peerPublicKey, kid);
				addAllPeerIdCredForX509Cred(cred_x509, peerPublicKey, serializedCert, x5uLink);
			}
		}
	}

}
