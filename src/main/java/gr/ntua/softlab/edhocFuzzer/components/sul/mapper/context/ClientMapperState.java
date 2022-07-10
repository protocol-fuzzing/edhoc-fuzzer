package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.AuthenticationConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
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
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.*;
import org.eclipse.californium.oscore.HashMapCtxDB;

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

	// The database of OSCORE Security Contexts
	protected HashMapCtxDB db = new HashMapCtxDB();

	// The size of the Replay Window to use in an OSCORE Recipient Context
	protected int OSCORE_REPLAY_WINDOW = 32;

	// The size to consider for MAX_UNFRAGMENTED SIZE
	protected int MAX_UNFRAGMENTED_SIZE = 4096;

	protected EdhocSession edhocSession;
	protected EdhocEndpointInfo edhocEndpointInfo;

	public ClientMapperState(String edhocURI, AuthenticationConfig authenticationConfig) {
		// edhocURI should be coap://localhost:port/.well-known/edhoc

		// Insert security providers
		Security.insertProviderAt(EdDSA, 1);
		Security.insertProviderAt(BouncyCastle, 2);

		// Enable EDHOC stack with EDHOC and OSCORE layers
		EdhocCoapStackFactory.useAsDefault(db, edhocSessions, peerPublicKeys, peerCredentials, usedConnectionIds,
				OSCORE_REPLAY_WINDOW, MAX_UNFRAGMENTED_SIZE);

		// Set authentication params
		this.authenticationMethod = authenticationConfig.getAuthenticationMethod();
		this.credType = authenticationConfig.getCredType();
		this.idCredType = authenticationConfig.getIdCredType();

		// Add the supported cipher suites in decreasing order of preference
		this.supportedCipherSuites.addAll(authenticationConfig.getSupportedCipherSuites());

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
		boolean useMessage4 = true;

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

		// Add one authentication credential for curve Ed25519 (SIG) and one for curve X25519 (STAT)

		if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_0) ||
				supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_1)) {

			// Curve Ed25519 (SIG)
			byte[] ed25519_privateKey = readEd25519PrivateDerFile(
					authenticationConfig.getEd25519KeySigConfig().getMapPrivateFilename());
			byte[] ed25519_publicKey = readEd25519PublicDerFile(
					authenticationConfig.getEd25519KeySigConfig().getMapPublicFilename());
			byte[] ed25519_cert = derFileToBytes(authenticationConfig.getEd25519KeySigConfig().getMapX509Filename());
			String ed25519_x5uLink = authenticationConfig.getEd25519KeySigConfig().getMapX5uLink();

			// If the type of credential identifier is 'kid', use 0x00,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x00
			byte[] ed25519_kid = new byte[] {(byte) 0x00};

			// Build key pair
			OneKey ed25519_keyPair = SharedSecretCalculation.buildEd25519OneKey(ed25519_privateKey, ed25519_publicKey);

			// Add the credentials
			addOwnCredentials(credType, idCredType, ed25519_kid, ed25519_keyPair, ed25519_cert,
					Constants.SIGNATURE_KEY, Constants.CURVE_Ed25519, subjectName, ed25519_x5uLink);

			// Curve X25519 (STAT)
			byte[] x25519_privateKey = readX25519PrivateDerFile(
					authenticationConfig.getX25519KeyStatConfig().getMapPrivateFilename());
			byte[] x25519_publicKey = readX25519PublicDerFile(
					authenticationConfig.getX25519KeyStatConfig().getMapPublicFilename());
			byte[] x25519_cert = derFileToBytes(authenticationConfig.getX25519KeyStatConfig().getMapX509Filename());
			String x25519_x5uLink = authenticationConfig.getX25519KeyStatConfig().getMapX5uLink();

			// If the type of credential identifier is 'kid', use 0x01,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x01
			byte[] x25519_kid = new byte[] {(byte) 0x01};

			// Build key pair
			OneKey x25519_keyPair = SharedSecretCalculation.buildCurve25519OneKey(x25519_privateKey, x25519_publicKey);

			// Add the credentials
			addOwnCredentials(credType, idCredType, x25519_kid, x25519_keyPair, x25519_cert,
					Constants.ECDH_KEY, Constants.CURVE_X25519, subjectName, x25519_x5uLink);
		}

		// Add two authentication credentials for curve P-256 (one for SIG and one for STAT)
		if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_2) ||
				supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_3)) {

			// P-256 (SIG)
			byte[] p256_sig_privateKey = readP256PrivateDerFile(
					authenticationConfig.getP256KeySigConfig().getMapPrivateFilename());
			byte[] p256_sig_publicKeyX = new byte[32];
			byte[] p256_sig_publicKeyY = new byte[32];
			readP256PublicDerFile(authenticationConfig.getP256KeySigConfig().getMapPublicFilename(),
					p256_sig_publicKeyX, p256_sig_publicKeyY);
			byte[] p256_sig_cert = derFileToBytes(authenticationConfig.getP256KeySigConfig().getMapX509Filename());
			String p256_sig_x5uLink = authenticationConfig.getP256KeySigConfig().getMapX5uLink();

			// If the type of credential identifier is 'kid', use 0x02,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x02
			byte[] p256_sig_kid = new byte[] {(byte) 0x02};

			// Build the key pair
			OneKey p256_sig_keyPair = SharedSecretCalculation.buildEcdsa256OneKey(p256_sig_privateKey,
					p256_sig_publicKeyX, p256_sig_publicKeyY);

			// Add the credentials
			addOwnCredentials(credType, idCredType, p256_sig_kid, p256_sig_keyPair, p256_sig_cert,
					Constants.SIGNATURE_KEY, Constants.CURVE_P256, subjectName, p256_sig_x5uLink);

			// P-256 (STAT)
			byte[] p256_stat_privateKey = readP256PrivateDerFile(
					authenticationConfig.getP256KeyStatConfig().getMapPrivateFilename());
			byte[] p256_stat_publicKeyX = new byte[32];
			byte[] p256_stat_publicKeyY = new byte[32];
			readP256PublicDerFile(authenticationConfig.getP256KeyStatConfig().getMapPublicFilename(),
					p256_stat_publicKeyX, p256_stat_publicKeyY);
			byte[] p256_stat_cert = derFileToBytes(authenticationConfig.getP256KeyStatConfig().getMapX509Filename());
			String p256_stat_x5uLink = authenticationConfig.getP256KeyStatConfig().getMapX5uLink();

			// If the type of credential identifier is 'kid', use 0x03,
			// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x03
			byte[] p256_stat_kid = new byte[] {(byte) 0x03};

			// Build the key pair
			OneKey p256_stat_keyPair = SharedSecretCalculation.buildEcdsa256OneKey(p256_stat_privateKey,
					p256_stat_publicKeyX, p256_stat_publicKeyY);

			// Add the credentials
			addOwnCredentials(credType, idCredType, p256_stat_kid, p256_stat_keyPair, p256_stat_cert,
					Constants.ECDH_KEY, Constants.CURVE_P256, subjectName, p256_stat_x5uLink);
		}
	}

	protected void setupPeerAuthenticationCredentials (AuthenticationConfig authenticationConfig) {
		// The subject name used for the identity key of the other peer
		String subjectName = "";

		// Add other peers' authentication credentials for curve Ed25519 (SIG)
		byte[] ed25519_publicKey = readEd25519PublicDerFile(
				authenticationConfig.getEd25519KeySigConfig().getSulPublicFilename());
		byte[] ed25519_cert = derFileToBytes(authenticationConfig.getEd25519KeySigConfig().getSulX509Filename());
		String ed25519_x5uLink = authenticationConfig.getEd25519KeySigConfig().getSulX5uLink();

		// If the type of credential identifier is 'kid', use 0x07,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x07
		byte[] ed25519_kid = new byte[] {(byte) 0x07};

		// Build the keyPair only from public key
		OneKey ed25519_keyPair = SharedSecretCalculation.buildEd25519OneKey(null, ed25519_publicKey);

		// Add the credentials
		addPeerCredentials(ed25519_keyPair, ed25519_kid, ed25519_cert, subjectName, ed25519_x5uLink);

		// Add other peers' authentication credentials for curve X25519 (STAT)
		byte[] x25519_publicKey = readX25519PublicDerFile(
				authenticationConfig.getX25519KeyStatConfig().getSulPublicFilename());
		byte[] x25519_cert = derFileToBytes(authenticationConfig.getX25519KeyStatConfig().getSulX509Filename());
		String x25519_x5uLink = authenticationConfig.getX25519KeyStatConfig().getSulX5uLink();

		// If the type of credential identifier is 'kid', use 0x08,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x08
		byte[] x25519_kid = new byte[] {(byte) 0x08};

		// Build the keyPair only from public key
		OneKey x25519_keyPair = SharedSecretCalculation.buildCurve25519OneKey(null, x25519_publicKey);

		// Add the credentials
		addPeerCredentials(x25519_keyPair, x25519_kid, x25519_cert, subjectName, x25519_x5uLink);

		// Add other peers' authentication credentials for curve P-256 (SIG)
		byte[] p256_sig_publicKeyX = new byte[32];
		byte[] p256_sig_publicKeyY = new byte[32];
		readP256PublicDerFile(authenticationConfig.getP256KeySigConfig().getSulPublicFilename(),
				p256_sig_publicKeyX, p256_sig_publicKeyY);
		byte[] p256_sig_cert = derFileToBytes(authenticationConfig.getP256KeySigConfig().getSulX509Filename());
		String p256_sig_x5uLink = authenticationConfig.getP256KeySigConfig().getSulX5uLink();

		// If the type of credential identifier is 'kid', use 0x09,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x09
		byte[] p256_sig_kid = new byte[] {(byte) 0x09};

		// Build the keyPair from only the public key
		OneKey p256_sig_keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, p256_sig_publicKeyX,
				p256_sig_publicKeyY);

		// Add the credentials
		addPeerCredentials(p256_sig_keyPair, p256_sig_kid, p256_sig_cert, subjectName, p256_sig_x5uLink);

		// Add other peers' authentication credentials for curve P-256 (STAT)
		byte[] p256_stat_publicKeyX = new byte[32];
		byte[] p256_stat_publicKeyY = new byte[32];
		readP256PublicDerFile(authenticationConfig.getP256KeyStatConfig().getSulPublicFilename(),
				p256_stat_publicKeyX, p256_stat_publicKeyY);
		byte[] p256_stat_cert = derFileToBytes(authenticationConfig.getP256KeyStatConfig().getSulX509Filename());
		String p256_stat_x5uLink = authenticationConfig.getP256KeyStatConfig().getSulX5uLink();

		// If the type of credential identifier is 'kid', use 0x0a,
		// i.e. the serialized ID_CRED_X is 0xa1, 0x04, 0x41, 0x0a
		byte[] p256_stat_kid = new byte[] {(byte) 0x0a};

		// Build the keyPair from only the public key
		OneKey p256_stat_keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, p256_stat_publicKeyX,
				p256_stat_publicKeyY);

		// Add the credentials
		addPeerCredentials(p256_stat_keyPair, p256_stat_kid, p256_stat_cert, subjectName, p256_stat_x5uLink);
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
			case Constants.CRED_TYPE_CWT -> {
				return null;
			}
			case Constants.CRED_TYPE_CCS -> {
				CBORObject idCredKidCbor = CBORObject.FromObject(kid);
				return Util.buildCredRawPublicKeyCcs(keyPair, subjectName, idCredKidCbor);
			}
			case Constants.CRED_TYPE_X509 -> {
				// CRED, as serialization of a CBOR byte string wrapping the serialized certificate
				return CBORObject.FromObject(serializedCert).EncodeToBytes();
			}
			default -> {
				throw new IllegalStateException("Unexpected credType value: " + credType);
			}
		}
	}

	protected CBORObject buildIdCred(int idCredType, byte[] kid, byte[] cred, byte[] serializedCert, String x5uLink) {
		switch (idCredType) {
			case Constants.ID_CRED_TYPE_KID -> {
				return Util.buildIdCredKid(kid);
			}
			case Constants.ID_CRED_TYPE_CWT -> {
				// TODO
				return null;
			}
			case Constants.ID_CRED_TYPE_CCS -> {
				return Util.buildIdCredKccs(CBORObject.DecodeFromBytes(cred));
			}
			case Constants.ID_CRED_TYPE_X5T -> {
				return Util.buildIdCredX5t(serializedCert);
			}
			case Constants.ID_CRED_TYPE_X5U -> {
				return Util.buildIdCredX5u(x5uLink);
			}
			case Constants.ID_CRED_TYPE_X5CHAIN -> {
				return Util.buildIdCredX5chain(serializedCert);
			}
			default -> {
				throw new IllegalStateException("Unexpected credType value: " + credType);
			}
		}
	}

	protected void addOwnCredentials(int credType, int idCredType, byte[] kid, OneKey keyPair, byte[] serializedCert,
									 int keyUsage, int keyCurve, String subjectName, String x5uLink){
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

	protected void addPeerCredentials(OneKey peerPublicKey, byte[] kid, byte[] serializedCert, String subjectName,
									  String x5uLink) {
		// Build CRED as a CCS, and the corresponding ID_CRED as 'kccs' and 'kid'
		byte[] cred = buildCred(Constants.CRED_TYPE_CCS, kid, peerPublicKey, subjectName, serializedCert);

		// ID_CRED as 'kccs'
		CBORObject idCredkccs = buildIdCred(Constants.ID_CRED_TYPE_CCS, kid, cred, serializedCert, x5uLink);
		// ID_CRED as 'kid'
		CBORObject idCredkid = buildIdCred(Constants.ID_CRED_TYPE_KID, kid, cred, serializedCert, x5uLink);

		peerPublicKeys.put(idCredkccs, peerPublicKey);
		peerCredentials.put(idCredkccs, CBORObject.FromObject(cred));
		peerPublicKeys.put(idCredkid, peerPublicKey);
		peerCredentials.put(idCredkid, CBORObject.FromObject(cred));

		// Build CRED as an X.509 certificate, and the corresponding ID_CRED as 'x5t', 'x5u' and 'x5chain'
		cred = buildCred(Constants.CRED_TYPE_X509, kid, peerPublicKey, subjectName, serializedCert);

		// ID_CRED as 'x5t'
		CBORObject idCredx5t = buildIdCred(Constants.ID_CRED_TYPE_X5T, kid, cred, serializedCert, x5uLink);
		// ID_CRED as 'x5u'
		CBORObject idCredx5u = buildIdCred(Constants.ID_CRED_TYPE_X5U, kid, cred, serializedCert, x5uLink);
		// ID_CRED as 'x5chain'
		CBORObject idCredx5chain = buildIdCred(Constants.ID_CRED_TYPE_X5CHAIN, kid, cred, serializedCert, x5uLink);

		peerPublicKeys.put(idCredx5t, peerPublicKey);
		peerCredentials.put(idCredx5t, CBORObject.FromObject(cred));
		peerPublicKeys.put(idCredx5u, peerPublicKey);
		peerCredentials.put(idCredx5u, CBORObject.FromObject(cred));
		peerPublicKeys.put(idCredx5chain, peerPublicKey);
		peerCredentials.put(idCredx5chain, CBORObject.FromObject(cred));
	}

}
