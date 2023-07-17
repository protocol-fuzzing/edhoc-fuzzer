package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocEndpointInfoPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.AuthenticationConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.ManyFilesAuthenticationConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.keyconfigs.KeyConfig;
import com.upokecenter.cbor.CBORObject;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.util.BigIntegers;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.Constants;
import org.eclipse.californium.edhoc.SharedSecretCalculation;
import org.eclipse.californium.edhoc.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class ManyFilesAuthenticator implements Authenticator {

    protected int authenticationMethod;
    protected int ownCredType;
    protected int ownIdCredType;
    protected HashMap<Integer, HashMap<Integer, OneKey>> keyPairs;
    protected HashMap<Integer, HashMap<Integer, CBORObject>> idCreds;
    protected HashMap<Integer, HashMap<Integer, CBORObject>> creds;
    protected Set<CBORObject> ownIdCreds;
    protected int peerCredType;
    protected int peerIdCredType;
    protected HashMap<CBORObject, OneKey> peerPublicKeys;
    protected HashMap<CBORObject, CBORObject> peerCredentials;
    protected List<Integer> supportedCipherSuites;

    // cache read DER files, so as not to read them after each constructor call
    protected static final HashMap<String, byte[]> sharedDerFilesMap = new HashMap<>();

    protected ManyFilesAuthenticationConfig manyFilesAuthenticationConfig;

    public ManyFilesAuthenticator(AuthenticationConfig authenticationConfig,
                                  EdhocEndpointInfoPersistent edhocEndpointInfoPersistent,
                                  Set<CBORObject> ownIdCreds) {
        this.manyFilesAuthenticationConfig = authenticationConfig.getManyFilesAuthenticationConfig();
        this.authenticationMethod = manyFilesAuthenticationConfig.getMapAuthenticationMethod();
        this.ownCredType = authenticationConfig.getMapCredType();
        this.ownIdCredType = authenticationConfig.getMapIdCredType();
        this.peerCredType = authenticationConfig.getSulCredType();
        this.peerIdCredType = authenticationConfig.getSulIdCredType();
        this.keyPairs = edhocEndpointInfoPersistent.getKeyPairs();
        this.idCreds = edhocEndpointInfoPersistent.getIdCreds();
        this.creds = edhocEndpointInfoPersistent.getCreds();
        this.ownIdCreds = ownIdCreds;
        this.peerPublicKeys = edhocEndpointInfoPersistent.getPeerPublicKeys();
        this.peerCredentials = edhocEndpointInfoPersistent.getPeerCredentials();
        this.supportedCipherSuites = edhocEndpointInfoPersistent.getSupportedCipherSuites();
    }

    public void setupOwnAuthenticationCredentials() {
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
                    keyConfig = manyFilesAuthenticationConfig.getEd25519KeySigConfig();
                    privateKey = readEd25519PrivateDerFile(keyConfig.getMapPrivateFilename());
                    publicKey = readEd25519PublicDerFile(keyConfig.getMapPublicFilename());

                    // Build key pair
                    keyPair = SharedSecretCalculation.buildEd25519OneKey(privateKey, publicKey);

                    // Add the credentials
                    addOwnCredentials(ownCredType, ownIdCredType, keyPair, keyConfig,
                            Constants.SIGNATURE_KEY, Constants.CURVE_Ed25519, subjectName);
                }
                case Constants.EDHOC_AUTH_METHOD_2,  Constants.EDHOC_AUTH_METHOD_3 -> {
                    // Curve X25519 (STAT)
                    keyConfig = manyFilesAuthenticationConfig.getX25519KeyStatConfig();
                    privateKey = readX25519PrivateDerFile(keyConfig.getMapPrivateFilename());
                    publicKey = readX25519PublicDerFile(keyConfig.getMapPublicFilename());

                    // Build key pair
                    keyPair = SharedSecretCalculation.buildCurve25519OneKey(privateKey, publicKey);

                    // Add the credentials
                    addOwnCredentials(ownCredType, ownIdCredType, keyPair, keyConfig,
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
                    keyConfig = manyFilesAuthenticationConfig.getP256KeySigConfig();
                    privateKey = readP256PrivateDerFile(keyConfig.getMapPrivateFilename());
                    publicKeyX = new byte[32];
                    publicKeyY = new byte[32];
                    readP256PublicDerFile(keyConfig.getMapPublicFilename(), publicKeyX, publicKeyY);

                    // Build the key pair
                    keyPair = SharedSecretCalculation.buildEcdsa256OneKey(privateKey, publicKeyX, publicKeyY);

                    // Add the credentials
                    addOwnCredentials(ownCredType, ownIdCredType, keyPair, keyConfig,
                            Constants.SIGNATURE_KEY, Constants.CURVE_P256, subjectName);
                }
                case Constants.EDHOC_AUTH_METHOD_2, Constants.EDHOC_AUTH_METHOD_3 -> {
                    // P-256 (STAT)
                    keyConfig = manyFilesAuthenticationConfig.getP256KeyStatConfig();
                    privateKey = readP256PrivateDerFile(keyConfig.getMapPrivateFilename());
                    publicKeyX = new byte[32];
                    publicKeyY = new byte[32];
                    readP256PublicDerFile(keyConfig.getMapPublicFilename(), publicKeyX, publicKeyY);

                    // Build the key pair
                    keyPair = SharedSecretCalculation.buildEcdsa256OneKey(privateKey, publicKeyX, publicKeyY);

                    // Add the credentials
                    addOwnCredentials(ownCredType, ownIdCredType, keyPair, keyConfig,
                            Constants.ECDH_KEY, Constants.CURVE_P256, subjectName);
                }
                default -> throw new RuntimeException("Invalid authentication method: " + authenticationMethod);
            }
        }
    }

    public void setupPeerAuthenticationCredentials() {
        // Add as many authentication credentials are provided

        // The subject name used for the identity key of the other peer
        String subjectName = "";
        String publicKeyFilename;
        KeyConfig keyConfig;
        OneKey keyPair;
        byte[] publicKey, publicKeyX, publicKeyY;

        switch (authenticationMethod) {
            case Constants.EDHOC_AUTH_METHOD_0, Constants.EDHOC_AUTH_METHOD_2 -> {
                // Add other peers' authentication credentials for curve Ed25519 (SIG)
                keyConfig = manyFilesAuthenticationConfig.getEd25519KeySigConfig();
                publicKeyFilename = keyConfig.getSulPublicFilename();
                if (publicKeyFilename != null) {
                    publicKey = readEd25519PublicDerFile(publicKeyFilename);

                    // Build the keyPair only from public key
                    keyPair = SharedSecretCalculation.buildEd25519OneKey(null, publicKey);

                    // Add the credentials
                    addPeerCredentials(peerCredType, peerIdCredType, keyPair, keyConfig, subjectName);
                }

                // Add other peers' authentication credentials for curve P-256 (SIG)
                keyConfig = manyFilesAuthenticationConfig.getP256KeySigConfig();
                publicKeyFilename = keyConfig.getSulPublicFilename();
                if (publicKeyFilename != null) {
                    publicKeyX = new byte[32];
                    publicKeyY = new byte[32];
                    readP256PublicDerFile(publicKeyFilename, publicKeyX, publicKeyY);

                    // Build the keyPair from only the public key
                    keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, publicKeyX, publicKeyY);

                    // Add the credentials
                    addPeerCredentials(peerCredType, peerIdCredType, keyPair, keyConfig, subjectName);
                }
            }
            case Constants.EDHOC_AUTH_METHOD_1, Constants.EDHOC_AUTH_METHOD_3 -> {
                // Add other peers' authentication credentials for curve X25519 (STAT)
                keyConfig = manyFilesAuthenticationConfig.getX25519KeyStatConfig();
                publicKeyFilename = keyConfig.getSulPublicFilename();
                if (publicKeyFilename != null) {
                    publicKey = readX25519PublicDerFile(publicKeyFilename);

                    // Build the keyPair only from public key
                    keyPair = SharedSecretCalculation.buildCurve25519OneKey(null, publicKey);

                    // Add the credentials
                    addPeerCredentials(peerCredType, peerIdCredType, keyPair, keyConfig, subjectName);
                }

                // Add other peers' authentication credentials for curve P-256 (STAT)
                keyConfig = manyFilesAuthenticationConfig.getP256KeyStatConfig();
                publicKeyFilename = keyConfig.getSulPublicFilename();
                if (publicKeyFilename != null) {
                    publicKeyX = new byte[32];
                    publicKeyY = new byte[32];
                    readP256PublicDerFile(publicKeyFilename, publicKeyX, publicKeyY);

                    // Build the keyPair from only the public key
                    keyPair = SharedSecretCalculation.buildEcdsa256OneKey(null, publicKeyX, publicKeyY);

                    // Add the credentials
                    addPeerCredentials(peerCredType, peerIdCredType, keyPair, keyConfig, subjectName);
                }
            }
        }
    }

    protected byte[] derFileToBytes(String filename) {
        if (filename == null) {
            throw new RuntimeException("Null DER filename provided");
        }

        if (!sharedDerFilesMap.containsKey(filename)) {
            try {
                // cache in order not to read all over again
                sharedDerFilesMap.put(filename, Files.readAllBytes(Paths.get(filename)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return sharedDerFilesMap.get(filename);
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

        if (keyPair == null) {
            throw new RuntimeException("Null provided keyPair");
        }

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
        if (peerPublicKey == null) {
            throw new RuntimeException("Null provided peerPublicKey");
        }

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
        if (peerPublicKey == null) {
            throw new RuntimeException("Null provided peerPublicKey");
        }

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

        if (peerPublicKey == null) {
            throw new RuntimeException("Null provided peerPublicKey");
        }

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
                else if (idCredType == Constants.ID_CRED_TYPE_X5T || idCredType == Constants.ID_CRED_TYPE_X5U
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
