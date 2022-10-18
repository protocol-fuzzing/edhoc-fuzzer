package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocEndpointInfoPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.AuthenticationConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.TestVector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.TestVectorAuthenticationConfig;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.Constants;
import org.eclipse.californium.edhoc.SharedSecretCalculation;
import org.eclipse.californium.elements.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TestVectorAuthenticator implements Authenticator {

    protected int authenticationMethod;
    protected int credType;
    protected int idCredType;
    protected HashMap<Integer, HashMap<Integer, OneKey>> keyPairs;
    protected HashMap<Integer, HashMap<Integer, CBORObject>> idCreds;
    protected HashMap<Integer, HashMap<Integer, CBORObject>> creds;
    protected Set<CBORObject> ownIdCreds;
    protected HashMap<CBORObject, OneKey> peerPublicKeys;
    protected HashMap<CBORObject, CBORObject> peerCredentials;
    protected List<Integer> supportedCipherSuites;

    protected TestVectorAuthenticationConfig testVectorAuthenticationConfig;

    protected boolean isInitiator;

    public TestVectorAuthenticator(AuthenticationConfig authenticationConfig,
                                   EdhocEndpointInfoPersistent edhocEndpointInfoPersistent,
                                   Set<CBORObject> ownIdCreds, boolean isInitiator) {
        this.testVectorAuthenticationConfig = authenticationConfig.getTestVectorAuthenticationConfig();
        this.credType = authenticationConfig.getMapCredType();
        this.idCredType = authenticationConfig.getMapIdCredType();
        this.keyPairs = edhocEndpointInfoPersistent.getKeyPairs();
        this.idCreds = edhocEndpointInfoPersistent.getIdCreds();
        this.creds = edhocEndpointInfoPersistent.getCreds();
        this.ownIdCreds = ownIdCreds;
        this.peerPublicKeys = edhocEndpointInfoPersistent.getPeerPublicKeys();
        this.peerCredentials = edhocEndpointInfoPersistent.getPeerCredentials();
        this.supportedCipherSuites = edhocEndpointInfoPersistent.getSupportedCipherSuites();
        this.isInitiator = isInitiator;
    }

    @Override
    public void setupOwnAuthenticationCredentials() {
        keyPairs.put(Constants.SIGNATURE_KEY, new HashMap<>());
        keyPairs.put(Constants.ECDH_KEY, new HashMap<>());
        creds.put(Constants.SIGNATURE_KEY, new HashMap<>());
        creds.put(Constants.ECDH_KEY, new HashMap<>());
        idCreds.put(Constants.SIGNATURE_KEY, new HashMap<>());
        idCreds.put(Constants.ECDH_KEY, new HashMap<>());

        OneKey keyPair;
        TestVector testVector = testVectorAuthenticationConfig.getTestVector();
        byte[] privateKey = StringUtil.hex2ByteArray(testVector.getPrivateKey(isInitiator));
        byte[] publicKey = StringUtil.hex2ByteArray(testVector.getPublicKey(isInitiator));

        if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_0) ||
                supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_1)) {
            switch (authenticationMethod) {
                case Constants.EDHOC_AUTH_METHOD_0, Constants.EDHOC_AUTH_METHOD_1 -> {
                    // Curve Ed25519 (SIG)
                    keyPair = SharedSecretCalculation.buildEd25519OneKey(privateKey, publicKey);
                    addOwnCredentials(Constants.SIGNATURE_KEY, Constants.CURVE_Ed25519, keyPair);
                }
                case Constants.EDHOC_AUTH_METHOD_2, Constants.EDHOC_AUTH_METHOD_3 -> {
                    // Curve X25519 (STAT)
                    keyPair = SharedSecretCalculation.buildCurve25519OneKey(privateKey, publicKey);
                    addOwnCredentials(Constants.ECDH_KEY, Constants.CURVE_X25519, keyPair);
                }
                default -> throw new RuntimeException("Invalid authentication method: " + authenticationMethod);
            }
        }

        if (supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_2) ||
                supportedCipherSuites.contains(Constants.EDHOC_CIPHER_SUITE_3)) {
            byte[] publicKeyX = new byte[32];
            byte[] publicKeyY = new byte[32];
            splitP256PublicKey(publicKey, publicKeyX, publicKeyY);
            keyPair = SharedSecretCalculation.buildEcdsa256OneKey(privateKey, publicKeyX, publicKeyY);

            switch (authenticationMethod) {
                case Constants.EDHOC_AUTH_METHOD_0, Constants.EDHOC_AUTH_METHOD_1 ->
                    // P-256 (SIG)
                    addOwnCredentials(Constants.SIGNATURE_KEY, Constants.CURVE_P256, keyPair);

                case Constants.EDHOC_AUTH_METHOD_2, Constants.EDHOC_AUTH_METHOD_3 ->
                    // P-256 (STAT)
                    addOwnCredentials(Constants.ECDH_KEY, Constants.CURVE_P256, keyPair);

                default -> throw new RuntimeException("Invalid authentication method: " + authenticationMethod);
            }
        }
    }

    @Override
    public void setupPeerAuthenticationCredentials() {
        OneKey keyPair;
        TestVector testVector = testVectorAuthenticationConfig.getTestVector();
        byte[] publicKey = StringUtil.hex2ByteArray(testVector.getPublicKey(!isInitiator));
        byte[] publicKeyX, publicKeyY;

        switch (authenticationMethod) {
            case Constants.EDHOC_AUTH_METHOD_0, Constants.EDHOC_AUTH_METHOD_2 -> {
                String keyCurve = testVectorAuthenticationConfig.getTestVectorPeerKeyCurve();
                if (Objects.equals(keyCurve, "P256")) {
                    // Try P-256 (SIG)
                    publicKeyX = new byte[32];
                    publicKeyY = new byte[32];
                    splitP256PublicKey(publicKey, publicKeyX, publicKeyY);
                    keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, publicKeyX, publicKeyY);
                    addPeerCredentials(keyPair);
                }
                else if (Objects.equals(keyCurve, "Ed25519")) {
                    // Curve Ed25519 (SIG)
                    keyPair = SharedSecretCalculation.buildEd25519OneKey(null, publicKey);
                    addPeerCredentials(keyPair);
                }
                else {
                    throw new RuntimeException("Invalid authentication method (" + authenticationMethod +
                            ") and peerKeyCurve (" + keyCurve + ") pair");
                }
            }

            case Constants.EDHOC_AUTH_METHOD_1, Constants.EDHOC_AUTH_METHOD_3 -> {
                String keyCurve = testVectorAuthenticationConfig.getTestVectorPeerKeyCurve();
                if (Objects.equals(keyCurve, "P256")) {
                    // Try P-256 (STAT)
                    publicKeyX = new byte[32];
                    publicKeyY = new byte[32];
                    splitP256PublicKey(publicKey, publicKeyX, publicKeyY);
                    keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, publicKeyX, publicKeyY);
                    addPeerCredentials(keyPair);
                }
                else if (Objects.equals(keyCurve, "X25519")) {
                    // Curve X25519 (STAT)
                    keyPair = SharedSecretCalculation.buildCurve25519OneKey(null, publicKey);
                    addPeerCredentials(keyPair);
                }
                else {
                    throw new RuntimeException("Invalid authentication method (" + authenticationMethod +
                            ") and peerKeyCurve (" + keyCurve + ") pair");
                }
            }
        }
    }

    protected void splitP256PublicKey(byte[] publicKey, byte[] publicX, byte[] publicY) {
        if (publicKey.length != 65) {
            throw new RuntimeException("Raw P256 public key should be in uncompressed format; [1B flag, 32B X, 32B Y]");
        }

        System.arraycopy(publicKey, 1, publicX, 0, publicX.length);
        System.arraycopy(publicKey, 1 + publicKey.length / 2, publicY, 0, publicY.length);
    }

    protected void addOwnCredentials(int keyUsage, int keyCurve, OneKey keyPair) {
        if (keyPair == null) {
            throw new RuntimeException("Null provided keyPair");
        }

        TestVector testVector = testVectorAuthenticationConfig.getTestVector();
        CBORObject cred = CBORObject.FromObject(StringUtil.hex2ByteArray(testVector.getCredCbor(isInitiator)));
        CBORObject idCred = CBORObject.DecodeFromBytes(StringUtil.hex2ByteArray(testVector.getIdCredCbor(isInitiator)));

        // Add the key pair, CRED and ID_CRED to the respective collections
        keyPairs.get(keyUsage).put(keyCurve, keyPair);
        creds.get(keyUsage).put(keyCurve, cred);
        idCreds.get(keyUsage).put(keyCurve, idCred);

        // Add this ID_CRED to the whole collection of ID_CRED_X for this peer
        ownIdCreds.add(idCred);
    }

    protected void addPeerCredentials(OneKey keyPair) {
        if (keyPair == null) {
            throw new RuntimeException("Null provided keyPair");
        }

        TestVector testVector = testVectorAuthenticationConfig.getTestVector();
        CBORObject cred = CBORObject.FromObject(StringUtil.hex2ByteArray(testVector.getCredCbor(!isInitiator)));
        CBORObject idCred = CBORObject.DecodeFromBytes(StringUtil.hex2ByteArray(testVector.getIdCredCbor(!isInitiator)));

        peerPublicKeys.put(idCred, keyPair);
        peerCredentials.put(idCred, cred);
    }
}
