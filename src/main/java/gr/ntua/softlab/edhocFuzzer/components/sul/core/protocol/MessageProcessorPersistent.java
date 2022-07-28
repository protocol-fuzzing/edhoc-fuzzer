package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocMapperState;
import net.i2p.crypto.eddsa.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.cose.*;
import org.eclipse.californium.edhoc.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Taken and heavily adapted from {@link org.eclipse.californium.edhoc.MessageProcessor}.
 * Substitutes the static functions of {@link org.eclipse.californium.edhoc.MessageProcessor} mainly by removing
 * field cleanup and purge session calls and by delaying changes to session. The read functions return boolean.
 * These functions are no longer static and take their parameters also from the class's field edhocMapperState.
 */
public class MessageProcessorPersistent {
    private static final Logger LOGGER = LogManager.getLogger(MessageProcessorPersistent.class);
    protected EdhocMapperState edhocMapperState;

    public MessageProcessorPersistent (EdhocMapperState edhocMapperState) {
        this.edhocMapperState = edhocMapperState;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.Util#nicePrint} */
    protected String byteArrayToString(String header, byte[] content) {
        String headerStr = header + " (" + content.length + " bytes):";

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n").append(headerStr).append("\n");

        String contentStr = Utils.bytesToHex(content);
        for (int i = 0; i < (content.length * 2); i++) {
            if ((i != 0) && (i % 20) == 0) {
                stringBuilder.append("\n");
            }

            stringBuilder.append(contentStr.charAt(i));

            if ((i % 2) == 1) {
                stringBuilder.append(" ");
            }
        }

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    /** Tries to match the byte sequence's structure of CBOR elements with an edhoc message */
    public int messageTypeFromStructure(byte[] sequence, boolean isReq) {
        LOGGER.debug("Start of messageTypeFromStructure");
        if (sequence == null) {
            return -1;
        }

        CBORObject[] elements;
        try {
            elements = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (CBORException e) {
            LOGGER.debug(e.getMessage());
            return -1;
        }

        // check if it matches error message structure
        if (isErrorMessage(elements, isReq)) {
            return Constants.EDHOC_ERROR_MESSAGE;
        }

        // if is request then C_I or C_R is prepended before the actual message
        int cX_offset = isReq ? 1 : 0;
        int messageElementsLength = elements.length - cX_offset;

        switch (messageElementsLength) {
            case 4, 5 -> {
                // message 1 has 4 or 5 elements with EAD_1
                return Constants.EDHOC_MESSAGE_1;
            }
            case 2 -> {
                // message 2 has 2 elements
                return Constants.EDHOC_MESSAGE_2;
            }
            case 1 -> {
                // message 3 and 4 have 1 element
                // they cannot be structurally matched
                return Constants.EDHOC_MESSAGE_3;
            }
            default -> {
                // unknown message structure
                return -1;
            }
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage1} */
    public byte[] writeMessage1() {
        LOGGER.debug("Start of writeMessage1");
        EdhocSession session = edhocMapperState.getEdhocSession();
        CBORObject[] ead1 = ((EdhocSessionPersistent) session).getEad1();

        // Prepare the list of CBOR objects to build the CBOR sequence
        List<CBORObject> objectList = new ArrayList<>();

        // C_X equal to the CBOR simple value 'true' (i.e. 0xf5)
        // if EDHOC message_1 is transported in a CoAP request
        if (session.isClientInitiated()) {
            objectList.add(CBORObject.True);
        }

        // METHOD as CBOR integer
        int method = session.getMethod();
        CBORObject method_cbor = CBORObject.FromObject(method);
        LOGGER.debug(byteArrayToString("METHOD", method_cbor.EncodeToBytes()));
        objectList.add(method_cbor);

        // SUITES_I as CBOR integer or CBOR array
        List<Integer> supportedCipherSuites = session.getSupportedCipherSuites();
        List<Integer> peerSupportedCipherSuites = session.getPeerSupportedCipherSuites();

        int selectedSuite = -1;
        int preferredSuite = supportedCipherSuites.get(0);

        if (peerSupportedCipherSuites == null) {
            // No SUITES_R has been received, so it is not known what cipher suites the responder supports
            // The selected cipher suite is the most preferred by the initiator
            selectedSuite = preferredSuite;
        } else {
            // SUITES_R has been received, so it is known what cipher suites the responder supports
            // Pick the selected cipher suite as the most preferred by the Initiator from the ones
            // supported by the Responder
            for (Integer i : supportedCipherSuites) {
                if (peerSupportedCipherSuites.contains(i)) {
                    selectedSuite = i;
                    break;
                }
            }
        }

        if (selectedSuite == -1) {
            LOGGER.debug("ERROR: Impossible to agree on a mutually supported cipher suite");
            return null;
        }

        // Set the selected cipher suite
        session.setSelectedCipherSuite(selectedSuite);

        // Set the asymmetric key pair, CRED and ID_CRED of the Initiator to use in this session
        session.setAuthenticationCredential();

        // Set the ephemeral keys of the Initiator to use in this session
        if (session.getEphemeralKey() == null)
            session.setEphemeralKey();

        CBORObject suitesI;
        if (selectedSuite == preferredSuite) {
            // SUITES_I is only the selected suite, as a CBOR integer
            suitesI = CBORObject.FromObject(selectedSuite);
        } else {
            // SUITES_I is a CBOR array
            // The elements are the Initiator's supported cipher suite in decreasing order of preference,
            // up until and including the selected suite as last element of the array.
            suitesI = CBORObject.NewArray();
            for (Integer i : supportedCipherSuites) {
                int suite = i;
                suitesI.Add(suite);
                if (suite == selectedSuite) {
                    break;
                }
            }
        }
        LOGGER.debug(byteArrayToString("SUITES_I", suitesI.EncodeToBytes()));
        objectList.add(suitesI);

        // G_X as a CBOR byte string
        CBORObject gX = switch(selectedSuite) {
            case Constants.EDHOC_CIPHER_SUITE_0, Constants.EDHOC_CIPHER_SUITE_1 ->
                    session.getEphemeralKey().PublicKey().get(KeyKeys.OKP_X);
            case Constants.EDHOC_CIPHER_SUITE_2, Constants.EDHOC_CIPHER_SUITE_3 ->
                    session.getEphemeralKey().PublicKey().get(KeyKeys.EC2_X);
            default ->
                null;
        };

        if (gX == null) {
            LOGGER.debug("ERROR: Invalid G_X");
            return null;
        }

        objectList.add(gX);
        LOGGER.debug(byteArrayToString("G_X", gX.GetByteString()));

        // C_I
        byte[] connectionIdentifierInitiator = session.getConnectionId();
        CBORObject cI = encodeIdentifier(connectionIdentifierInitiator);
        LOGGER.debug(byteArrayToString("Connection Identifier of the Initiator", connectionIdentifierInitiator));
        LOGGER.debug(byteArrayToString("C_I", cI.EncodeToBytes()));
        objectList.add(cI);

        // EAD_1, if provided
        if (ead1 != null) {
            Collections.addAll(objectList, ead1);
        }

        /* Prepare EDHOC Message 1 */
        byte[] message1 = Util.buildCBORSequence(objectList);
        LOGGER.debug(byteArrayToString("EDHOC Message 1", message1));

        // Compute and store the hash of Message 1
        // In case of CoAP request the first byte 0xf5 must be skipped
        int offset = session.isClientInitiated() ? 1 : 0;
        byte[] message1_hash = new byte[message1.length - offset];
        System.arraycopy(message1, offset, message1_hash, 0, message1_hash.length);

        /* Modify session */
        session.setHashMessage1(message1_hash);

        return message1;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage1} */
    public boolean readMessage1(byte[] sequence, boolean isReq) {
        LOGGER.debug("Start of checkAndReadMessage1");
        List<Integer> supportedCipherSuites = edhocMapperState.getEdhocEndpointInfo().getSupportedCipherSuites();
        AppProfile appProfile = edhocMapperState.getEdhocSession().getApplicationProfile();

        if (sequence == null || supportedCipherSuites == null || appProfile == null) {
            LOGGER.debug("ERROR: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.debug("ERROR: Unable to decode byte sequence to CBOR object array");
            return false;
        }

        /* Consistency checks */

        if (objectListRequest.length == 0) {
            LOGGER.debug("ERROR: CBOR object array is empty");
            return false;
        }

        // If the received message is a request (i.e. the CoAP client is the initiator), the first element
        // before the actual message_1 is the CBOR simple value 'true', i.e. the byte 0xf5, and it can be skipped
        if (isReq) {
            index++;
            if (!objectListRequest[index].equals(CBORObject.True)) {
                LOGGER.debug("ERROR: The first element must be the CBOR simple value 'true'");
                return false;
            }
        }

        // METHOD
        index++;
        if (objectListRequest[index].getType() != CBORType.Integer) {
            LOGGER.debug("ERROR: Method must be an integer");
            return false;
        }

        // Check that the indicated authentication method is supported
        int method = objectListRequest[index].AsInt32();
        if (!appProfile.isAuthMethodSupported(method)) {
            LOGGER.debug("ERROR: Authentication method '{}' is not supported", method);
            return false;
        }

        // SUITES_I
        index++;
        if (objectListRequest[index].getType() == CBORType.Integer
                && objectListRequest[index].AsInt32() < 0) {
            LOGGER.debug("ERROR: SUITES_I as an integer must be positive");
            return false;
        } else if (objectListRequest[index].getType() == CBORType.Array) {
            if (objectListRequest[index].size() < 2) {
                LOGGER.debug("ERROR: SUITES_I as an array must have at least 2 elements");
                return false;
            }

            for (int i = 0; i < objectListRequest[index].size(); i++) {
                if(objectListRequest[index].get(i).getType() != CBORType.Integer
                        || objectListRequest[index].get(i).AsInt32() < 0) {
                    LOGGER.debug("ERROR: SUITES_I as an array must have positive integers as elements");
                    return false;
                }
            }
        } else {
            // SUITES_I is not cbor_integer nor cbor_array
            LOGGER.debug("ERROR: SUITES_I must be integer or array");
            return false;
        }

        // Skip checking if the selected cipher suite is supported and that no prior cipher suite in SUITES_I
        // is supported, because then a specific error message should follow up, but the learner is responsible
        // for picking and sending new messages

        // G_X
        index++;
        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.debug("ERROR: G_X must be a byte string");
            return false;
        }

        // C_I
        index++;
        if (objectListRequest[index].getType() != CBORType.ByteString
                && objectListRequest[index].getType() != CBORType.Integer) {
            LOGGER.debug("ERROR: C_I must be a byte string or an integer");
            return false;
        }

        // The Connection Identifier C_I as encoded in the EDHOC message
        CBORObject cI = objectListRequest[index];
        byte[] connectionIdentifierInitiator = decodeIdentifier(cI);
        if (connectionIdentifierInitiator == null) {
            LOGGER.debug("ERROR: Invalid encoding of C_I");
            return false;
        }

        LOGGER.debug(byteArrayToString("Connection Identifier of the Initiator", connectionIdentifierInitiator));
        LOGGER.debug(byteArrayToString("C_I", cI.EncodeToBytes()));

        // EAD_1
        index++;
        CBORObject[] ead1 = null;
        if (objectListRequest.length > index) {
            // EAD_1 is present
            int length = objectListRequest.length - index;

            if ((length % 2) == 1) {
                LOGGER.debug("ERROR: EAD_1 should have even length");
                return false;
            } else {
                ead1 = new CBORObject[length];
                int eadIndex = 0;

                for (int i = index; i < objectListRequest.length; i++) {
                    if ((eadIndex % 2) == 0 && objectListRequest[i].getType() != CBORType.Integer) {
                        LOGGER.debug("ERROR: Processing EAD_1 on integer");
                        return false;
                    }

                    if ((eadIndex % 2) == 1 && objectListRequest[i].getType() != CBORType.ByteString) {
                        LOGGER.debug("ERROR: Processing EAD_1 on byte string");
                        return false;
                    }

                    // Make a hard copy
                    byte[] serializedObject = objectListRequest[i].EncodeToBytes();
                    CBORObject element = CBORObject.DecodeFromBytes(serializedObject);
                    ead1[eadIndex] = element;
                    eadIndex++;
                }
            }
        }

        ((EdhocSessionPersistent) edhocMapperState.getEdhocSession()).setEad1(ead1);
        LOGGER.debug("Successful processing of EDHOC Message 1");
        return true;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage2} */
    public byte[] writeMessage2() {
        LOGGER.debug("Start of writeMessage2");
        EdhocSession session = edhocMapperState.getEdhocSession();
        CBORObject[] ead2 = ((EdhocSessionPersistent) session).getEad2();
        List<CBORObject> objectList = new ArrayList<>();

        // C_I, if EDHOC message_2 is transported in a CoAP request
        if (!session.isClientInitiated()) {
            byte[] connectionIdentifierInitiator = session.getPeerConnectionId();
            CBORObject cI = encodeIdentifier(connectionIdentifierInitiator);
            LOGGER.debug(byteArrayToString("Connection Identifier of the Initiator", connectionIdentifierInitiator));
            LOGGER.debug(byteArrayToString("C_I", cI.EncodeToBytes()));
            objectList.add(cI);
        }

        // Set the ephemeral keys to use in this session
        if (session.getEphemeralKey() == null) {
            session.setEphemeralKey();
        }

        // G_Y as a CBOR byte string
        int selectedSuite = session.getSelectedCipherSuite();
        CBORObject gY = switch(selectedSuite) {
            case Constants.EDHOC_CIPHER_SUITE_0, Constants.EDHOC_CIPHER_SUITE_1 ->
                    session.getEphemeralKey().PublicKey().get(KeyKeys.OKP_X);
            case Constants.EDHOC_CIPHER_SUITE_2, Constants.EDHOC_CIPHER_SUITE_3 ->
                    session.getEphemeralKey().PublicKey().get(KeyKeys.EC2_X);
            default ->
                null;
        };

        if (gY == null) {
            LOGGER.debug("ERROR: Invalid G_Y");
            return null;
        }
        LOGGER.debug(byteArrayToString("G_Y", gY.GetByteString()));

        // C_R
        byte[] connectionIdentifierResponder = session.getConnectionId();
        CBORObject cR = encodeIdentifier(connectionIdentifierResponder);
        LOGGER.debug(byteArrayToString("Connection Identifier of the Responder", connectionIdentifierResponder));
        LOGGER.debug(byteArrayToString("C_R", cR.EncodeToBytes()));

        // Compute TH_2
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(selectedSuite);
        byte[] hashMessage1 = session.getHashMessage1();
        byte[] hashMessage1SerializedCBOR = CBORObject.FromObject(hashMessage1).EncodeToBytes();
        byte[] gYSerializedCBOR = gY.EncodeToBytes();
        byte[] cRSerializedCBOR = cR.EncodeToBytes();
        byte[] th2 = computeTH2(hashAlgorithm, gYSerializedCBOR, cRSerializedCBOR, hashMessage1SerializedCBOR);

        if (th2 == null) {
            LOGGER.debug("ERROR: Computing TH_2");
            return null;
        }

        LOGGER.debug(byteArrayToString("H(message_1)", hashMessage1));
        LOGGER.debug(byteArrayToString("TH_2", th2));


        // Compute the key material

        // Compute the Diffie-Hellman secret G_XY
        byte[] dhSecret = SharedSecretCalculation.generateSharedSecret(session.getEphemeralKey(),
                session.getPeerEphemeralPublicKey());

        if (dhSecret == null) {
            LOGGER.debug("ERROR: Computing the Diffie-Hellman Secret");
            return null;
        }
        LOGGER.debug(byteArrayToString("G_XY", dhSecret));

        // Compute PRK_2e
        byte[] prk2e = computePRK2e(dhSecret, hashAlgorithm);

        if (prk2e == null) {
            LOGGER.debug("ERROR: Computing PRK_2e");
            return null;
        }
        LOGGER.debug(byteArrayToString("PRK_2e", prk2e));

        // Compute PRK_3e2m
        byte[] prk3e2m = computePRK3e2m(session, prk2e, th2, session.getPeerLongTermPublicKey(),
                session.getPeerEphemeralPublicKey());

        if (prk3e2m == null) {
            LOGGER.debug("ERROR: Computing PRK_3e2m");
            return null;
        }
        LOGGER.debug(byteArrayToString("PRK_3e2m", prk3e2m));

        /* Start computing Signature_or_MAC_2 */

        // Compute MAC_2
        byte[] mac2 = computeMAC2(session, prk3e2m, th2, session.getIdCred(), session.getCred(), ead2);
        if (mac2 == null) {
            LOGGER.debug("ERROR: Computing MAC_2");
            return null;
        }
        LOGGER.debug(byteArrayToString("MAC_2", mac2));

        // Compute Signature_or_MAC_2

        // Compute the external data for the external_aad, as a CBOR sequence
        byte[] externalData = computeExternalData(th2, session.getCred(), ead2);
        if (externalData == null) {
            LOGGER.debug("ERROR; Computing the external data for MAC_2");
            return null;
        }

        byte[] signatureOrMac2 = computeSignatureOrMac2(session, mac2, externalData);

        if (signatureOrMac2 == null) {
            LOGGER.debug("ERROR: Computing Signature_or_MAC_2");
            return null;
        }
        LOGGER.debug(byteArrayToString("Signature_or_MAC_2", signatureOrMac2));

        /* End computing Signature_or_MAC_2 */

        /* Start computing CIPHERTEXT_2 */

        // Prepare the plaintext
        List<CBORObject> plaintextElementList = new ArrayList<>();
        CBORObject plaintextElement;

        if (session.getIdCred().ContainsKey(HeaderKeys.KID.AsCBOR())) {
            // ID_CRED_R uses 'kid', whose value is the only thing to include in the plaintext
            CBORObject kid = session.getIdCred().get(HeaderKeys.KID.AsCBOR()); // v-14 identifiers
            plaintextElement = encodeIdentifier(kid.GetByteString());  // v-14 identifiers
        } else {
            plaintextElement = session.getIdCred();
        }

        plaintextElementList.add(plaintextElement);
        plaintextElementList.add(CBORObject.FromObject(signatureOrMac2));

        if (ead2 != null) {
            Collections.addAll(plaintextElementList, ead2);
        }

        byte[] plaintext2 = Util.buildCBORSequence(plaintextElementList);
        LOGGER.debug(byteArrayToString("Plaintext to compute CIPHERTEXT_2", plaintext2));

        // Compute KEYSTREAM_2
        byte[] keystream2 = computeKeystream2(session, th2, prk2e, plaintext2.length);
        if (keystream2== null) {
            LOGGER.debug("ERROR: Computing KEYSTREAM_2");
            return null;
        }
        LOGGER.debug(byteArrayToString("KEYSTREAM_2", keystream2));

        // Compute CIPHERTEXT_2
        byte[] ciphertext2 = Util.arrayXor(plaintext2, keystream2);

        LOGGER.debug(byteArrayToString("CIPHERTEXT_2", ciphertext2));
        /* End computing CIPHERTEXT_2 */

        // Finish building the outer CBOR sequence

        // Concatenate G_Y with CIPHERTEXT_2
        byte[] gY_Ciphertext2 = new byte[gY.GetByteString().length + ciphertext2.length];
        System.arraycopy(gY.GetByteString(), 0, gY_Ciphertext2, 0, gY.GetByteString().length);
        System.arraycopy(ciphertext2, 0, gY_Ciphertext2, gY.GetByteString().length, ciphertext2.length);

        // Wrap the result in a single CBOR byte string, included in the outer CBOR sequence of EDHOC Message 2
        objectList.add(CBORObject.FromObject(gY_Ciphertext2));
        LOGGER.debug(byteArrayToString("G_Y | CIPHERTEXT_2", gY_Ciphertext2));

        // The outer CBOR sequence finishes with the connection identifier C_R
        objectList.add(cR);

        /* Modify session */
        session.setTH2(th2);
        session.setPRK2e(prk2e);
        session.setPRK3e2m(prk3e2m);
        session.setPlaintext2(plaintext2);

        /* Prepare EDHOC Message 2 */
        byte[] message2 = Util.buildCBORSequence(objectList);
        LOGGER.debug(byteArrayToString("EDHOC Message 2", message2));
        return message2;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage2} */
    public boolean readMessage2(byte[] sequence, boolean isReq, byte[] connectionIdInitiator) {
        LOGGER.debug("Start of checkAndReadMessage2");
        HashMap<CBORObject, EdhocSession> edhocSessions = edhocMapperState.getEdhocEndpointInfo().getEdhocSessions();
        HashMap<CBORObject, OneKey> peerPublicKeys = edhocMapperState.getEdhocEndpointInfo().getPeerPublicKeys();
        HashMap<CBORObject, CBORObject> peerCredentials = edhocMapperState.getEdhocEndpointInfo().getPeerCredentials();
        Set<CBORObject> usedConnectionIds = edhocMapperState.getEdhocEndpointInfo().getUsedConnectionIds();
        Set<CBORObject> ownIdCreds = edhocMapperState.getOwnIdCreds();

        if (sequence == null || edhocSessions == null || peerPublicKeys == null || peerCredentials == null
                || usedConnectionIds == null) {
            LOGGER.debug("Error: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.debug("ERROR: Unable to decode byte sequence to CBOR object array");
            return false;
        }

        /* Consistency checks */

        // C_I
        byte[] connectionIdentifierInitiator;
        if (!isReq) {
            connectionIdentifierInitiator = connectionIdInitiator;
        } else {
            // If EDHOC Message 2 is transported in a CoAP request
            // C_I is present as first element of the CBOR sequence
            index++;
            CBORObject cI = objectListRequest[index];

            if (cI.getType() != CBORType.ByteString && cI.getType() != CBORType.Integer)  {
                LOGGER.debug("ERROR: C_I must be a byte string or an integer");
                return false;
            }

            connectionIdentifierInitiator = decodeIdentifier(cI);

            if (connectionIdentifierInitiator == null) {
                LOGGER.debug("ERROR: Invalid encoding of C_I");
                return false;
            }
        }

        LOGGER.debug(byteArrayToString("Connection Identifier of the Initiator", connectionIdentifierInitiator));

        CBORObject connectionIdentifierInitiatorCbor = CBORObject.FromObject(connectionIdentifierInitiator);
        EdhocSession session = edhocSessions.get(connectionIdentifierInitiatorCbor);

        if (session == null) {
            LOGGER.debug("ERROR: EDHOC session not found");
            return false;
        }

        // G_Y | CIPHERTEXT_2
        index++;

        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.debug("ERROR: (G_Y | CIPHERTEXT_2) must be a byte string");
            return false;
        }

        byte[] gY_Ciphertext2 = objectListRequest[index].GetByteString();

        int gYLength = EdhocSession.getEphermeralKeyLength(session.getSelectedCipherSuite());
        int ciphertext2Length = gY_Ciphertext2.length - gYLength;

        if (ciphertext2Length <= 0) {
            LOGGER.debug("ERROR: CIPHERTEXT_2 has non-positive size");
            return false;
        }

        // G_Y
        byte[] gY = new byte[gYLength];
        System.arraycopy(gY_Ciphertext2, 0, gY, 0, gYLength);

        LOGGER.debug(byteArrayToString("G_Y", gY));

        // Ephemeral public key of the Responder
        int selectedCipherSuite = session.getSelectedCipherSuite();

        OneKey peerEphemeralKey = switch(selectedCipherSuite) {
            case Constants.EDHOC_CIPHER_SUITE_0, Constants.EDHOC_CIPHER_SUITE_1 ->
                    SharedSecretCalculation.buildCurve25519OneKey(null, gY);
            case Constants.EDHOC_CIPHER_SUITE_2, Constants.EDHOC_CIPHER_SUITE_3 ->
                    SharedSecretCalculation.buildEcdsa256OneKey(null, gY, null);
            default ->
                    null;
        };

        if (peerEphemeralKey == null) {
            LOGGER.debug("ERROR: Invalid ephemeral public key G_Y");
            return false;
        }

        LOGGER.debug(byteArrayToString("PeerEphemeralPublicKey", peerEphemeralKey.AsCBOR().EncodeToBytes()));

        // CIPHERTEXT_2
        byte[] ciphertext2 = new byte[ciphertext2Length];
        System.arraycopy(gY_Ciphertext2, gYLength, ciphertext2, 0, ciphertext2Length);
        LOGGER.debug(byteArrayToString("CIPHERTEXT_2", ciphertext2));

        // C_R
        index++;
        CBORObject cR = objectListRequest[index];

        if (cR.getType() != CBORType.ByteString && cR.getType() != CBORType.Integer) {
            LOGGER.debug("ERROR: C_R must be a byte string or an integer");
            return false;
        }

        byte[] connectionIdentifierResponder = decodeIdentifier(cR);
        if (connectionIdentifierResponder == null) {
            LOGGER.debug("ERROR: Invalid encoding of C_R");
            return false;
        }

        LOGGER.debug(byteArrayToString("Connection Identifier of the Responder", connectionIdentifierResponder));
        LOGGER.debug(byteArrayToString("C_R", cR.EncodeToBytes()));

        if (session.getApplicationProfile().getUsedForOSCORE()
                && Arrays.equals(connectionIdentifierInitiator, connectionIdentifierResponder)) {
            LOGGER.debug("ERROR: C_R must be different from C_I");
            return false;
        }

        /* Decrypt CIPHERTEXT_2 */

        // Compute TH2
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());
        byte[] hashMessage1 = session.getHashMessage1();
        byte[] hashMessage1SerializedCBOR = CBORObject.FromObject(hashMessage1).EncodeToBytes();
        byte[] gYSerializedCBOR = CBORObject.FromObject(gY).EncodeToBytes();
        byte[] cRSerializedCBOR = cR.EncodeToBytes();
        byte[] th2 = computeTH2(hashAlgorithm, gYSerializedCBOR, cRSerializedCBOR, hashMessage1SerializedCBOR);

        if (th2 == null) {
            LOGGER.debug("ERROR: Computing TH2");
            return false;
        }

        LOGGER.debug(byteArrayToString("H(message_1)", hashMessage1));
        LOGGER.debug(byteArrayToString("TH_2", th2));

        // Compute the Diffie-Hellman secret G_XY
        byte[] dhSecret = SharedSecretCalculation.generateSharedSecret(session.getEphemeralKey(), peerEphemeralKey);

        if (dhSecret == null) {
            LOGGER.debug("ERROR: Computing the Diffie-Hellman secret G_XY");
            return false;
        }

        LOGGER.debug(byteArrayToString("G_XY", dhSecret));

        // Compute PRK_2e
        byte[] prk2e = computePRK2e(dhSecret, hashAlgorithm);

        if (prk2e == null) {
            LOGGER.debug("ERROR: Computing PRK_2e");
            return false;
        }

        LOGGER.debug(byteArrayToString("PRK_2e", prk2e));

        // Compute KEYSTREAM_2
        byte[] keystream2 = computeKeystream2(session, th2, prk2e, ciphertext2.length);
        if (keystream2 == null) {
            LOGGER.debug("ERROR: Computing KEYSTREAM_2");
            return false;
        }

        LOGGER.debug(byteArrayToString("KEYSTREAM_2", keystream2));

        // Compute the plaintext
        byte[] plaintext2 = Util.arrayXor(ciphertext2, keystream2);
        LOGGER.debug(byteArrayToString("Plaintext retrieved from CIPHERTEXT_2", plaintext2));

        // Parse the plaintext as a CBOR sequence
        int baseIndex = 0;
        CBORObject[] plaintextElementList;
        try {
            plaintextElementList = CBORObject.DecodeSequenceFromBytes(plaintext2);
        } catch (Exception e) {
            LOGGER.debug("ERROR: Malformed or invalid CBOR encoded plaintext from CIPHERTEXT_2");
            return false;
        }

        if (plaintextElementList.length == 0) {
            LOGGER.debug("ERROR: Zero-length plaintext_2");
            return false;
        }

        // Discard possible padding prepended to the plaintext
        while (baseIndex < plaintextElementList.length
                && plaintextElementList[baseIndex] == CBORObject.True) {
            baseIndex++;
        }

        // ID_CRED_R and Signature_or_MAC_2 should be contained
        if (plaintextElementList.length - baseIndex < 2) {
            LOGGER.debug("ERROR: Plaintext_2 contains less than two elements");
            return false;
        }

        // check ID_CRED_R
        if (plaintextElementList[baseIndex].getType() != CBORType.ByteString
                && plaintextElementList[baseIndex].getType() != CBORType.Integer
                && plaintextElementList[baseIndex].getType() != CBORType.Map) {
            LOGGER.debug("ERROR: Invalid type of ID_CRED_R in plaintext_2");
            return false;
        }

        // check Signature_or_MAC_2
        if (plaintextElementList[baseIndex + 1].getType() != CBORType.ByteString) {
            LOGGER.debug("ERROR: Signature_or_MAC_2 must be a byte string");
            return false;
        }

        // check EAD_2
        CBORObject[] ead2 = null;
        if (plaintextElementList.length - baseIndex > 2) {
            // EAD_2 is present
            int length = plaintextElementList.length - baseIndex - 2;

            if ((length % 2) == 1) {
                LOGGER.debug("ERROR: EAD_2 should have even length");
                return false;
            }

            ead2 = new CBORObject[length];
            int eadIndex = 0;

            for (int i = baseIndex + 2; i < plaintextElementList.length; i++) {
                if ((eadIndex % 2) == 0 && plaintextElementList[i].getType() != CBORType.Integer) {
                    LOGGER.debug("ERROR: Processing EAD_2 on integer");
                    return false;
                }
                if ((eadIndex % 2) == 1 && plaintextElementList[i].getType() != CBORType.ByteString) {
                    LOGGER.debug("ERROR: Processing EAD_2 on byte string");
                    return false;
                }

                // Make a hard copy
                byte[] serializedObject = plaintextElementList[i].EncodeToBytes();
                CBORObject element = CBORObject.DecodeFromBytes(serializedObject);
                ead2[eadIndex] = element;
                eadIndex++;
            }
        }

        // Verify that the identity of the Responder is an allowed identity
        CBORObject idCredR = CBORObject.NewMap();
        CBORObject rawIdCredR = plaintextElementList[baseIndex];

        // ID_CRED_R is a CBOR map with 'kid', and only 'kid' was transported
        if (rawIdCredR.getType() == CBORType.ByteString || rawIdCredR.getType() == CBORType.Integer) {
            byte[] kidValue = decodeIdentifier(rawIdCredR);
            idCredR.Add(HeaderKeys.KID.AsCBOR(), kidValue);
        } else if (rawIdCredR.getType() == CBORType.Map) {
            idCredR = rawIdCredR;
        } else {
            LOGGER.debug("ERROR: Invalid format for ID_CRED_R");
            return false;
        }

        if (!peerPublicKeys.containsKey(idCredR)) {
            LOGGER.debug("ERROR: The identity expressed by ID_CRED_R is not recognized");
            return false;
        }

        OneKey peerLongTermKey = peerPublicKeys.get(idCredR);

        if (ownIdCreds.contains(idCredR)) {
            LOGGER.debug("ERROR: The identity expressed by ID_CRED_R is equal to my own identity");
            return false;
        }

        // Compute PRK_3e2m
        byte[] prk3e2m = computePRK3e2m(session, prk2e, th2, peerEphemeralKey, peerLongTermKey);
        if (prk3e2m == null) {
            LOGGER.debug("ERROR: Computing PRK_3e2m");
            return false;
        }

        LOGGER.debug(byteArrayToString("PRK_3e2m", prk3e2m));

        /* Start verifying Signature_or_MAC_2 */

        CBORObject peerCredentialCBOR = peerCredentials.get(idCredR);
        if (peerCredentialCBOR == null) {
            LOGGER.debug("ERROR: Unable to retrieve the peer credential");
            return false;
        }

        byte[] peerCredential = peerCredentialCBOR.GetByteString();

        // Compute MAC_2
        byte[] mac2 = computeMAC2(session, prk3e2m, th2, idCredR, peerCredential, ead2);
        if (mac2 == null) {
            LOGGER.debug("ERROR: Computing MAC_2");
            return false;
        }

        LOGGER.debug(byteArrayToString("MAC_2", mac2));

        // Verify Signature_or_MAC_2
        byte[] signatureOrMac2 = plaintextElementList[baseIndex + 1].GetByteString();
        LOGGER.debug(byteArrayToString("Signature_or_MAC_2", signatureOrMac2));

        // Prepare the External Data, as a CBOR sequence
        byte[] externalData = computeExternalData(th2, peerCredential, ead2);
        if (externalData == null) {
            LOGGER.debug("ERROR: Computing External Data for MAC_2");
            return false;
        }

        LOGGER.debug(byteArrayToString("External Data to verify Signature_or_MAC_2", externalData));

        if (!verifySignatureOrMac2(session, peerLongTermKey, idCredR, signatureOrMac2, externalData, mac2)) {
            LOGGER.debug("ERROR: Non valid Signature_or_MAC_2");
            return false;
        }

        /* Modify session */
        session.setPeerEphemeralPublicKey(peerEphemeralKey);
        session.setPeerConnectionId(connectionIdentifierResponder);
        session.setTH2(th2);
        session.setPRK2e(prk2e);
        session.setPeerIdCred(idCredR);
        session.setPeerLongTermPublicKey(peerLongTermKey);
        session.setPRK3e2m(prk3e2m);
        session.setPlaintext2(plaintext2);
        ((EdhocSessionPersistent) session).setEad2(ead2);

        LOGGER.debug("Successful processing of EDHOC Message 2");
        return true;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage3} */
    public byte[] writeMessage3() {
        LOGGER.debug("Start of writeMessage3");
        EdhocSession session = edhocMapperState.getEdhocSession();
        CBORObject[] ead3 = ((EdhocSessionPersistent) session).getEad3();
        List<CBORObject> objectList = new ArrayList<>();

        /* Start preparing data_3 */

        // C_R, if EDHOC message_3 is transported in a CoAP request
        if (session.isClientInitiated()) {
            byte[] connectionIdentifierResponder = session.getPeerConnectionId();
            CBORObject cR = encodeIdentifier(connectionIdentifierResponder);
            LOGGER.debug(byteArrayToString("Connection Identifier of the Responder",
                    connectionIdentifierResponder));
            LOGGER.debug(byteArrayToString("C_R", cR.EncodeToBytes()));
            objectList.add(cR);
            }

        /* End preparing data_3 */

        /* Start computing the inner COSE object */

        // Compute TH_3
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());
        byte[] th2 = session.getTH2();
        byte[] th2SerializedCBOR = CBORObject.FromObject(th2).EncodeToBytes();
        byte[] plaintext2 = session.getPlaintext2();
        byte[] th3 = computeTH3(hashAlgorithm, th2SerializedCBOR, plaintext2);

        if (th3 == null) {
            LOGGER.debug("ERROR: Computing TH_3");
            return null;
        }

        LOGGER.debug(byteArrayToString("TH_3", th3));

        // Compute the key material
        byte[] prk4e3m = computePRK4e3m(session, session.getPRK3e2m(), th3, session.getPeerLongTermPublicKey(),
                session.getPeerEphemeralPublicKey());

        if (prk4e3m == null) {
            LOGGER.debug("ERROR: Computing PRK_4e3m");
            return null;
        }
        LOGGER.debug(byteArrayToString("PRK_4e3m", prk4e3m));

        /* Start computing Signature_or_MAC_3 */

        // Compute MAC_3
        byte[] mac3 = computeMAC3(session, prk4e3m, th3, session.getIdCred(), session.getCred(), ead3);

        if (mac3 == null) {
            LOGGER.debug("ERROR: Computing MAC_3");
            return null;
        }
        LOGGER.debug(byteArrayToString("MAC_3", mac3));

        // Compute Signature_or_MAC_3

        // Compute the external data for the external_aad, as a CBOR sequence
        byte[] externalData = computeExternalData(th3, session.getCred(), ead3);
        if (externalData == null) {
            LOGGER.debug("ERROR: Computing the external data for MAC_3");
            return null;
        }

        byte[] signatureOrMac3 = computeSignatureOrMac3(session, mac3, externalData);
        if (signatureOrMac3 == null) {
            LOGGER.debug("ERROR: Computing Signature_or_MAC_3");
            return null;
        }
        LOGGER.debug(byteArrayToString("Signature_or_MAC_3", signatureOrMac3));

        /* End computing Signature_or_MAC_3 */

        /* Start computing CIPHERTEXT_3 */

        // Compute K_3 and IV_3 to protect the outer COSE object

        byte[] k3 = computeKeyOrIV3("KEY", session, th3, session.getPRK3e2m());
        if (k3 == null) {
            LOGGER.debug("ERROR: Computing K_3");
            return null;
        }
        LOGGER.debug(byteArrayToString("K_3", k3));

        byte[] iv3 = computeKeyOrIV3("IV", session, th3, session.getPRK3e2m());
        if (iv3 == null) {
            LOGGER.debug("ERROR: Computing IV_3");
            return null;
        }
        LOGGER.debug(byteArrayToString("IV_3", iv3));

        // Prepare the External Data as including only TH3
        externalData = th3;

        // Prepare the plaintext
        List<CBORObject> plaintextElementList = new ArrayList<>();
        CBORObject plaintextElement;

        if (session.getIdCred().ContainsKey(HeaderKeys.KID.AsCBOR())) {
            // ID_CRED_I uses 'kid', whose value is the only thing to include in the plaintext
            CBORObject kid = session.getIdCred().get(HeaderKeys.KID.AsCBOR());
            plaintextElement = encodeIdentifier(kid.GetByteString());
        } else {
            plaintextElement = session.getIdCred();
        }

        plaintextElementList.add(plaintextElement);
        plaintextElementList.add(CBORObject.FromObject(signatureOrMac3));
        if (ead3 != null) {
            Collections.addAll(plaintextElementList, ead3);
        }

        byte[] plaintext3 = Util.buildCBORSequence(plaintextElementList);
        LOGGER.debug(byteArrayToString("Plaintext to compute CIPHERTEXT_3", plaintext3));

        // Compute CIPHERTEXT_3 and add it to the outer CBOR sequence

        byte[] ciphertext3 = computeCiphertext3(session.getSelectedCipherSuite(), externalData, plaintext3, k3, iv3);
        LOGGER.debug(byteArrayToString("CIPHERTEXT_3", ciphertext3));
        objectList.add(CBORObject.FromObject(ciphertext3));

        /* End computing CIPHERTEXT_3 */

        /* Compute TH4 */

        byte[] th3SerializedCBOR = CBORObject.FromObject(th3).EncodeToBytes();
        byte[] th4 = computeTH4(hashAlgorithm, th3SerializedCBOR, plaintext3);
        if (th4 == null) {
            LOGGER.debug("ERROR: Computing TH_4");
            return null;
        }
        LOGGER.debug(byteArrayToString("TH_4", th4));

        /* Compute PRK_out */
        byte[] prkOut = computePRKout(session, th4, prk4e3m);
        if (prkOut == null) {
            LOGGER.debug("ERROR: Computing PRK_out");
            return null;
        }
        LOGGER.debug(byteArrayToString("PRK_out", prkOut));

        /* Compute PRK_exporter */
        byte[] prkExporter = computePRKexporter(session, prkOut);
        if (prkExporter == null) {
            LOGGER.debug("ERROR: Computing PRK_exporter");
            return null;
        }
        LOGGER.debug(byteArrayToString("PRK_exporter", prkExporter));

        /* Modify session */
        session.setTH3(th3);
        session.setPRK4e3m(prk4e3m);
        session.setTH4(th4);
        session.setPRKout(prkOut);
        session.setPRKexporter(prkExporter);

        /* Prepare EDHOC Message 3 */
        byte[] message3 = Util.buildCBORSequence(objectList);
        LOGGER.debug(byteArrayToString("EDHOC Message 3", message3));
        return message3;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage3} */
    public boolean readMessage3(byte[] sequence, boolean isReq, byte[] connectionIdResponder) {
        LOGGER.debug("Start of checkAndReadMessage3");
        HashMap<CBORObject, EdhocSession> edhocSessions = edhocMapperState.getEdhocEndpointInfo().getEdhocSessions();
        HashMap<CBORObject, OneKey> peerPublicKeys = edhocMapperState.getEdhocEndpointInfo().getPeerPublicKeys();
        HashMap<CBORObject, CBORObject> peerCredentials = edhocMapperState.getEdhocEndpointInfo().getPeerCredentials();
        Set<CBORObject> usedConnectionIds = edhocMapperState.getEdhocEndpointInfo().getUsedConnectionIds();

        if (sequence == null || edhocSessions == null || peerPublicKeys == null || peerCredentials == null
                || usedConnectionIds == null) {
            LOGGER.debug("ERROR: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.debug("ERROR: Unable to decode byte sequence to CBOR object array");
            return false;
        }


        /* Consistency checks */

        // C_R
        byte[] connectionIdentifierResponder;
        if (!isReq) {
            connectionIdentifierResponder = connectionIdResponder;
        } else {
            // If EDHOC Message 3 is transported in a CoAP request,
            // C_R is present as first element of the CBOR sequence
            index++;
            if (objectListRequest[index].getType() != CBORType.ByteString
                    && objectListRequest[index].getType() != CBORType.Integer)  {
                LOGGER.debug("ERROR: C_R must be a byte string or an integer");
                return false;
            }

            connectionIdentifierResponder = decodeIdentifier(objectListRequest[index]);  // v-14 identifiers
            if (connectionIdentifierResponder == null) {
                LOGGER.debug("ERROR: Invalid encoding of C_R");
                return false;
            }
        }

        CBORObject connectionIdentifierResponderCbor = CBORObject.FromObject(connectionIdentifierResponder);
        EdhocSession session = edhocSessions.get(connectionIdentifierResponderCbor);

        if (session == null) {
            LOGGER.debug("ERROR: EDHOC session not found");
            return false;
        }

        // CIPHERTEXT_3
        index++;
        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.debug("ERROR: CIPHERTEXT_3 must be a byte string");
            return false;
        }
        byte[] ciphertext3 = objectListRequest[index].GetByteString();
        LOGGER.debug(byteArrayToString("CIPHERTEXT_3", ciphertext3));

        /* Decrypt CIPHERTEXT_3 */

        // Compute TH3
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());
        byte[] th2 = session.getTH2();
        byte[] th2SerializedCBOR = CBORObject.FromObject(th2).EncodeToBytes();
        byte[] plaintext2 = session.getPlaintext2();
        byte[] th3 = computeTH3(hashAlgorithm, th2SerializedCBOR, plaintext2);

        if (th3 == null) {
            LOGGER.debug("ERROR: Computing TH3");
            return false;
        }
        LOGGER.debug(byteArrayToString("TH_3", th3));

        // Compute K_3 and IV_3 to protect the outer COSE object
        byte[] k3 = computeKeyOrIV3("KEY", session, th3, session.getPRK3e2m());
        if (k3 == null) {
            LOGGER.debug("ERROR: Computing TH3");
            return false;
        }
        LOGGER.debug(byteArrayToString("K_3", k3));


        byte[] iv3 = computeKeyOrIV3("IV", session, th3, session.getPRK3e2m());
        if (iv3 == null) {
            LOGGER.debug("ERROR: Computing IV_3ae");
            return false;
        }
        LOGGER.debug(byteArrayToString("IV_3", iv3));

        // Prepare the external_aad as including only TH3
        byte[] externalData = th3;

        // Compute the plaintext
        byte[] plaintext3 = decryptCiphertext3(session.getSelectedCipherSuite(), externalData, ciphertext3, k3, iv3);
        if (plaintext3 == null) {
            LOGGER.debug("ERROR: Decrypting CIPHERTEXT_3");
            return false;
        }
        LOGGER.debug(byteArrayToString("Plaintext retrieved from CIPHERTEXT_3", plaintext3));

        // Parse the outer plaintext as a CBOR sequence
        int baseIndex = 0;
        CBORObject[] plaintextElementList;
        try {
            plaintextElementList = CBORObject.DecodeSequenceFromBytes(plaintext3);
        } catch (Exception e) {
            LOGGER.debug("ERROR: Malformed or invalid plaintext from CIPHERTEXT_3");
            return false;
        }

        if (plaintextElementList.length == 0) {
            LOGGER.debug("ERROR: Zero-length plaintext_3");
            return false;
        }

        // Discard possible padding prepended to the plaintext
        while (baseIndex < plaintextElementList.length
                && plaintextElementList[baseIndex] == CBORObject.True) {
            baseIndex++;
        }

        // ID_CRED_I and Signature_or_MAC_3 should be contained
        if (plaintextElementList.length - baseIndex < 2) {
            LOGGER.debug("ERROR: Plaintext_3 contains less than two elements");
            return false;
        }

        // check ID_CRED_I
        if (plaintextElementList[baseIndex].getType() != CBORType.ByteString
                && plaintextElementList[baseIndex].getType() != CBORType.Integer
                && plaintextElementList[baseIndex].getType() != CBORType.Map) {
            LOGGER.debug("ERROR: Invalid type of ID_CRED_I in plaintext_3");
            return false;
        }

        // check Signature_or_MAC_3
        if (plaintextElementList[baseIndex + 1].getType() != CBORType.ByteString) {
            LOGGER.debug("ERROR: Signature_or_MAC_3 must be a byte string");
            return false;
        }

        // check EAD_3
        CBORObject[] ead3 = null;
        if (plaintextElementList.length - baseIndex > 2) {
            // EAD_3 is present
            int length = plaintextElementList.length - baseIndex - 2;

            if ((length % 2) == 1) {
                LOGGER.debug("ERROR: EAD_3 should have even length");
                return false;
            }

            ead3 = new CBORObject[length];

            int eadIndex = 0;

            for (int i = baseIndex + 2; i < plaintextElementList.length; i++) {
                if ((eadIndex % 2) == 0 && plaintextElementList[i].getType() != CBORType.Integer) {
                    LOGGER.debug("ERROR: Processing EAD_3 on integer");
                    return false;
                }
                if ((eadIndex % 2) == 1 && plaintextElementList[i].getType() != CBORType.ByteString) {
                    LOGGER.debug("ERROR: Processing EAD_2 on byte string");
                    return false;
                }

                // Make a hard copy
                byte[] serializedObject = plaintextElementList[i].EncodeToBytes();
                CBORObject element = CBORObject.DecodeFromBytes(serializedObject);
                ead3[eadIndex] = element;
                eadIndex++;
            }
        }

        // Verify that the identity of the Initiator is an allowed identity
        CBORObject idCredI = CBORObject.NewMap();
        CBORObject rawIdCredI = plaintextElementList[0];

        // ID_CRED_I is a CBOR map with 'kid', and only 'kid' was transported
        if (rawIdCredI.getType() == CBORType.ByteString || rawIdCredI.getType() == CBORType.Integer) {
            byte[] kidValue = decodeIdentifier(rawIdCredI);
            idCredI.Add(HeaderKeys.KID.AsCBOR(), kidValue);
        } else if (rawIdCredI.getType() == CBORType.Map) {
            idCredI = rawIdCredI;
        } else {
            LOGGER.debug("ERROR: Invalid format for ID_CRED_I");
            return false;
        }

        if (!peerPublicKeys.containsKey(idCredI)) {
            LOGGER.debug("ERROR: The identity expressed by ID_CRED_I is not recognized");
            return false;
        }
        OneKey peerLongTermKey = peerPublicKeys.get(idCredI);

        CBORObject peerCredentialCBOR = peerCredentials.get(idCredI);
        if (peerCredentialCBOR == null) {
            LOGGER.debug("ERROR: Unable to retrieve the peer credential");
            return false;
        }

        byte[] peerCredential = peerCredentialCBOR.GetByteString();

        // Compute the key material
        byte[] prk4e3m = computePRK4e3m(session, session.getPRK3e2m(), th3, peerLongTermKey,
                session.getPeerEphemeralPublicKey());
        if (prk4e3m == null) {
            LOGGER.debug("ERROR: Computing PRK_4e3m");
            return false;
        }
        LOGGER.debug(byteArrayToString("PRK_4e3m", prk4e3m));

        /* Start verifying Signature_or_MAC_3 */

        // Compute MAC_3
        byte[] mac3 = computeMAC3(session, prk4e3m, th3, idCredI, peerCredential, ead3);
        if (mac3 == null) {
            LOGGER.debug("ERROR: Computing MAC_3");
            return false;
        }
        LOGGER.debug(byteArrayToString("MAC_3", mac3));

        // Verify Signature_or_MAC_3

        byte[] signatureOrMac3 = plaintextElementList[1].GetByteString();
        LOGGER.debug(byteArrayToString("Signature_or_MAC_3", signatureOrMac3));

        // Compute the external data, as a CBOR sequence
        externalData = computeExternalData(th3, peerCredential, ead3);
        if (externalData == null) {
            LOGGER.debug("ERROR: Computing the external data for MAC_3");
            return false;
        }
        LOGGER.debug(byteArrayToString("External Data to verify Signature_or_MAC_3", externalData));

        if (!verifySignatureOrMac3(session, peerLongTermKey, idCredI, signatureOrMac3, externalData, mac3)) {
            LOGGER.debug("ERROR: Non valid Signature_or_MAC_3");
            return false;
        }

        /* End verifying Signature_or_MAC_3 */

        /* Compute TH4 */

        byte[] th3SerializedCBOR = CBORObject.FromObject(th3).EncodeToBytes();
        byte[] th4 = computeTH4(hashAlgorithm, th3SerializedCBOR, plaintext3);
        if (th4 == null) {
            LOGGER.debug("ERROR: Computing TH_4");
            return false;
        }
        LOGGER.debug(byteArrayToString("TH_4", th4));

        /* Compute PRK_out */
        byte[] prkOut = computePRKout(session, th4, prk4e3m);
        if (prkOut == null) {
            LOGGER.debug("ERROR: Computing PRK_out");
            return false;
        }

        LOGGER.debug(byteArrayToString("PRK_out", prkOut));

        /* Compute PRK_exporter */
        byte[] prkExporter = computePRKexporter(session, prkOut);
        if (prkExporter == null) {
            LOGGER.debug("ERROR: Computing PRK_exporter");
            return false;
        }
        LOGGER.debug(byteArrayToString("PRK_exporter", prkExporter));

        /* Modify session */
        session.setTH3(th3);
        session.setPeerIdCred(idCredI);
        session.setPeerLongTermPublicKey(peerLongTermKey);
        session.setPRK4e3m(prk4e3m);
        session.setTH4(th4);
        session.setPRKout(prkOut);
        session.setPRKexporter(prkExporter);
        ((EdhocSessionPersistent) session).setEad3(ead3);

        LOGGER.debug("Successful processing of EDHOC Message 3");
        return true;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage4} */
    public byte[] writeMessage4() {
        LOGGER.debug("Start of writeMessage4");
        EdhocSession session = edhocMapperState.getEdhocSession();
        CBORObject[] ead4 = ((EdhocSessionPersistent) session).getEad4();
        List<CBORObject> objectList = new ArrayList<>();

        /* Start preparing data_4 */

        // C_I, if EDHOC message_4 is transported in a CoAP request
        if (!session.isClientInitiated()) {
            byte[] connectionIdentifierInitiator = session.getPeerConnectionId();
            CBORObject cI = encodeIdentifier(connectionIdentifierInitiator);
            objectList.add(cI);
            LOGGER.debug(byteArrayToString("Connection Identifier of the Initiator",
                    connectionIdentifierInitiator));
            LOGGER.debug(byteArrayToString("C_I", cI.EncodeToBytes()));
        }

        /* End preparing data_4 */

        /* Start computing the COSE object */

        // Compute the external data for the external_aad

        // Prepare the External Data as including only TH4
        byte[] externalData = session.getTH4();

        if (externalData == null) {
            LOGGER.debug("ERROR: Computing the external data for CIPHERTEXT_4");
            return null;
        }
        LOGGER.debug(byteArrayToString("External Data to compute CIPHERTEXT_4", externalData));

        // Prepare the plaintext
        byte[] plaintext4 = new byte[] {};
        if (ead4 != null) {
            List<CBORObject> plaintextElementList = new ArrayList<>();
            Collections.addAll(plaintextElementList, ead4);
            plaintext4 = Util.buildCBORSequence(plaintextElementList);
        }
        LOGGER.debug(byteArrayToString("Plaintext to compute CIPHERTEXT_4", plaintext4));

        // Compute the key material

        // Compute K and IV to protect the COSE object
        byte[] k4 = computeKeyOrIV4("KEY", session, session.getTH4(), session.getPRK4e3m());
        if (k4 == null) {
            LOGGER.debug("ERROR: Computing K_4");
            return null;
        }
        LOGGER.debug(byteArrayToString("K_4", k4));

        byte[] iv4 = computeKeyOrIV4("IV", session, session.getTH4(), session.getPRK4e3m());
        if (iv4 == null) {
            LOGGER.debug("ERROR: Computing IV_4");
            return null;
        }
        LOGGER.debug(byteArrayToString("IV_4", iv4));

        // Encrypt the COSE object and take the ciphertext as CIPHERTEXT_4
        byte[] ciphertext4 = computeCiphertext4(session.getSelectedCipherSuite(), externalData, plaintext4, k4, iv4);
        if (ciphertext4 == null) {
            LOGGER.debug("ERROR: Computing CIPHERTEXT_4");
            return null;
        }
        LOGGER.debug(byteArrayToString("CIPHERTEXT_4", ciphertext4));
        objectList.add(CBORObject.FromObject(ciphertext4));

        /* End computing the inner COSE object */

        /* Prepare EDHOC Message 4 */
        byte[] message4 = Util.buildCBORSequence(objectList);
        LOGGER.debug(byteArrayToString("EDHOC Message 4", Util.buildCBORSequence(objectList)));

        return message4;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage4} */
    public boolean readMessage4(byte[] sequence, boolean isReq, byte[] connectionIdInitiator) {
        LOGGER.debug("Start of checkAndReadMessage4");
        HashMap<CBORObject, EdhocSession> edhocSessions = edhocMapperState.getEdhocEndpointInfo().getEdhocSessions();
        Set<CBORObject> usedConnectionIds = edhocMapperState.getEdhocEndpointInfo().getUsedConnectionIds();

        if (sequence == null || edhocSessions == null || usedConnectionIds == null) {
            LOGGER.debug("Error: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.debug("ERROR: Unable to decode byte sequence to CBOR object array");
            return false;
        }

        /* Consistency checks */

        // C_I
        byte[] connectionIdentifierInitiator;
        if (!isReq) {
            connectionIdentifierInitiator = connectionIdInitiator;
        } else {
            // If EDHOC Message 4 is transported in a CoAP request,
            // C_I is present as first element of the CBOR sequence
            index++;
            if (objectListRequest[index].getType() != CBORType.ByteString
                    && objectListRequest[index].getType() != CBORType.Integer)  {
                LOGGER.debug("ERROR: C_I must be a byte string or an integer");
                return false;
            }

            connectionIdentifierInitiator = decodeIdentifier(objectListRequest[index]);
            if (connectionIdentifierInitiator == null) {
                LOGGER.debug("ERROR: Invalid encoding of C_I");
                return false;
            }
        }

        CBORObject connectionIdentifierInitiatorCbor = CBORObject.FromObject(connectionIdentifierInitiator);
        EdhocSession session = edhocSessions.get(connectionIdentifierInitiatorCbor);

        if (session == null) {
            LOGGER.debug("ERROR: EDHOC session not found");
            return false;
        }

        // CIPHERTEXT_4
        index++;
        byte[] ciphertext4;
        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.debug("ERROR: CIPHERTEXT_4 must be a byte string");
            return false;
        }

        ciphertext4 = objectListRequest[index].GetByteString();
        if (ciphertext4 == null) {
            LOGGER.debug("ERROR: Retrieving CIPHERTEXT_4");
            return false;
        }
        LOGGER.debug(byteArrayToString("CIPHERTEXT_4", ciphertext4));

        /* Compute the plaintext */

        // Compute the external data for the external_aad

        // Prepare the External Data as including only TH4
        byte[] externalData = session.getTH4();

        if (externalData == null) {
            LOGGER.debug("ERROR: Computing the external data for CIPHERTEXT_4");
            return false;
        }
        LOGGER.debug(byteArrayToString("External Data to compute CIPHERTEXT_4", externalData));

        // Compute the key material

        // Compute K and IV to protect the COSE object

        byte[] k4ae = computeKeyOrIV4("KEY", session, session.getTH4(), session.getPRK4e3m());
        if (k4ae == null) {
            LOGGER.debug("ERROR: Computing K");
            return false;
        }
        LOGGER.debug(byteArrayToString("K", k4ae));

        byte[] iv4ae = computeKeyOrIV4("IV", session, session.getTH4(), session.getPRK4e3m());
        if (iv4ae == null) {
            LOGGER.debug("ERROR: Computing IV");
            return false;
        }
        LOGGER.debug(byteArrayToString("IV", iv4ae));

        byte[] plaintext4 = decryptCiphertext4(session.getSelectedCipherSuite(), externalData, ciphertext4, k4ae, iv4ae);
        if (plaintext4 == null) {
            LOGGER.debug("ERROR: Decrypting CIPHERTEXT_4");
            return false;
        }
        LOGGER.debug(byteArrayToString("Plaintext retrieved from CIPHERTEXT_4", plaintext4));

        /* End computing the plaintext */


        // Parse the outer plaintext as a CBOR sequence. To be valid, this is either the empty plaintext
        // or just padding or padding followed by the External Authorization Data EAD_4 possibly
        CBORObject[] ead4 = null;

        if (plaintext4.length > 0) {
            int baseIndex = 0;
            CBORObject[] plaintextElementList;

            try {
                plaintextElementList = CBORObject.DecodeSequenceFromBytes(plaintext4);
            } catch (Exception e) {
                LOGGER.debug("ERROR: Malformed or invalid EAD_4");
                return false;
            }

            // Discard possible padding prepended to the plaintext
            while (baseIndex < plaintextElementList.length
                    && plaintextElementList[baseIndex] == CBORObject.True) {
                baseIndex++;
            }

            if (plaintextElementList.length - baseIndex > 0) {
                // EAD_4 is present
                int length = plaintextElementList.length - baseIndex;

                if ((length % 2) == 1) {
                    LOGGER.debug("ERROR: EAD_4 should have even length");
                    return false;
                }

                ead4 = new CBORObject[length];

                int eadIndex = 0;

                for (int i = baseIndex; i < plaintextElementList.length; i++) {
                    if ((eadIndex % 2) == 0 && plaintextElementList[i].getType() != CBORType.Integer) {
                        LOGGER.debug("ERROR: Processing EAD_4 on integer");
                        return false;
                    }
                    if ((eadIndex % 2) == 1 && plaintextElementList[i].getType() != CBORType.ByteString) {
                        LOGGER.debug("ERROR: Processing EAD_3 on byte string");
                        return false;
                    }

                    // Make a hard copy
                    byte[] serializedObject = plaintextElementList[i].EncodeToBytes();
                    CBORObject element = CBORObject.DecodeFromBytes(serializedObject);
                    ead4[eadIndex] = element;
                    eadIndex++;
                }
            }
        }

        /* Modify session */
        ((EdhocSessionPersistent) session).setEad4(ead4);

        LOGGER.debug("Successful processing of EDHOC Message 4");
        return true;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#isErrorMessage} */
    protected boolean isErrorMessage(CBORObject[] myObjects, boolean isReq) {
        // A CoAP message including an EDHOC error message is a CBOR sequence of at least two elements
        if (myObjects.length < 2)
            return false;

        if (isReq) {
            // If in a request, this starts with C_X different than 'true' (0xf5),
            // followed by ERR_CODE as a CBOR integer
            return !myObjects[0].equals(CBORObject.True) && myObjects[1].getType() == CBORType.Integer;
        }
        else {
            // If in a response, this starts with ERR_CODE as a CBOR integer
            return myObjects[0].getType() == CBORType.Integer;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#writeErrorMessage} */
    public byte[] writeErrorMessage(int errorCode, int replyTo, boolean isErrorReq, byte[] connectionIdentifier,
                                    String errMsg, CBORObject suitesR) {

        if (replyTo != Constants.EDHOC_MESSAGE_1 && replyTo != Constants.EDHOC_MESSAGE_2 &&
                replyTo != Constants.EDHOC_MESSAGE_3 && replyTo != Constants.EDHOC_MESSAGE_4) {
            return null;
        }

        if (suitesR != null && suitesR.getType() != CBORType.Integer && suitesR.getType() != CBORType.Array)
            return null;

        if (suitesR != null && suitesR.getType() == CBORType.Array) {
            for (int i = 0 ; i < suitesR.size(); i++) {
                if (suitesR.get(i).getType() != CBORType.Integer)
                    return null;
            }
        }

        List<CBORObject> objectList = new ArrayList<>();
        byte[] payload;

        // Possibly include C_X - This might not have been included if the incoming EDHOC message was malformed
        if (connectionIdentifier != null && isErrorReq) {
            CBORObject cX = encodeIdentifier(connectionIdentifier);
            objectList.add(cX);
        }

        // Include ERR_CODE
        objectList.add(CBORObject.FromObject(errorCode));

        // Include ERR_INFO
        if (errorCode == Constants.ERR_CODE_UNSPECIFIED_ERROR) {
            if (errMsg == null)
                return null;
            // Include DIAG_MSG
            objectList.add(CBORObject.FromObject(errMsg));
        } else if (errorCode == Constants.ERR_CODE_WRONG_SELECTED_CIPHER_SUITE) {
            if (replyTo != Constants.EDHOC_MESSAGE_1)
                return null;

            // Possibly include SUITES_R
            // This implies that EDHOC Message 1 was good enough and yielded a suite negotiation
            if (suitesR != null)
                objectList.add(suitesR);
        }

        // Encode the EDHOC Error Message, as a CBOR sequence
        payload = Util.buildCBORSequence(objectList);

        LOGGER.debug("Successful preparation of EDHOC Error Message");
        return payload;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#encodeIdentifier} */
    protected CBORObject encodeIdentifier(byte[] identifier) {
        CBORObject identifierCBOR = null;

        if (identifier != null && identifier.length != 1) {
            // Encode the EDHOC connection identifier as a CBOR byte string
            identifierCBOR = CBORObject.FromObject(identifier);
        }

        if (identifier != null && identifier.length == 1) {
            int byteValue = Util.bytesToInt(identifier);

            if ((byteValue >= 0 && byteValue <= 23) || (byteValue >= 32 && byteValue <= 55)) {
                // The EDHOC connection identifier is in the range 0x00-0x17 or in the range 0x20-0x37.
                // That is, it happens to be the serialization of a CBOR integer with numeric value -24..23

                // Encode the EDHOC connection identifier as a CBOR integer
                identifierCBOR = CBORObject.DecodeFromBytes(identifier);
            }
            else {
                // Encode the EDHOC connection identifier as a CBOR byte string
                identifierCBOR = CBORObject.FromObject(identifier);
            }
        }

        return identifierCBOR;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#decodeIdentifier} */
    protected byte[] decodeIdentifier(CBORObject identifierCbor) {
        byte[] identifier = null;

        if (identifierCbor != null && identifierCbor.getType() == CBORType.ByteString) {
            identifier = identifierCbor.GetByteString();

            // Consistency check
            if (identifier.length == 1) {
                int byteValue = Util.bytesToInt(identifier);
                if ((byteValue >= 0 && byteValue <= 23) || (byteValue >= 32 && byteValue <= 55))
                    // This EDHOC connection identifier should have been encoded as a CBOR integer
                    identifier = null;
            }
        }
        else if (identifierCbor != null && identifierCbor.getType() == CBORType.Integer) {
            identifier = identifierCbor.EncodeToBytes();

            if (identifier.length != 1) {
                // This EDHOC connection identifier is not valid or was not encoded according to deterministic CBOR
                identifier = null;
            }

        }
        return identifier;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeTH2} */
    protected byte[] computeTH2(String hashAlgorithm, byte[] gY, byte[] cR, byte[] hashMessage1) {
        int offset = 0;
        byte[] hashInput = new byte[gY.length + cR.length + hashMessage1.length];
        System.arraycopy(gY, 0, hashInput, offset, gY.length);
        offset += gY.length;
        System.arraycopy(cR, 0, hashInput, offset, cR.length);
        offset += cR.length;
        System.arraycopy(hashMessage1, 0, hashInput, offset, hashMessage1.length);

        try {
            return Util.computeHash(hashInput, hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Invalid hash algorithm when computing TH2\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computePRK2e} */
    protected byte[] computePRK2e(byte[] dhSecret, String hashAlgorithm) {
        if (hashAlgorithm.equals("SHA-256") || hashAlgorithm.equals("SHA-384") || hashAlgorithm.equals("SHA-512")) {
            try {
                return Hkdf.extract(new byte[]{}, dhSecret);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                LOGGER.debug("ERROR: Generating PRK_2e\n" + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computePRK3e2m} */
    protected byte[] computePRK3e2m(EdhocSession session, byte[] prk2e, byte[] th2, OneKey peerLongTerm,
                                    OneKey peerEphemeral) {
        byte[] prk3e2m = null;
        int authenticationMethod = session.getMethod();
        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_2) {
            // The responder uses signatures as authentication method, then PRK_3e2m is equal to PRK_2e
            prk3e2m = new byte[prk2e.length];
            System.arraycopy(prk2e, 0, prk3e2m, 0, prk2e.length);
        }
        else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_1
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The responder does not use signatures as authentication method, then PRK_3e2m has to be computed
            byte[] dhSecret;
            OneKey privateKey;
            OneKey publicKey;

            if (session.isInitiator())  {
                // Use the long-term key of the Responder as public key
                publicKey = peerLongTerm;

                // Use the ephemeral key of the Initiator as private key
                privateKey = session.getEphemeralKey();

            } else {
                // Use the ephemeral key of the Initiator as public key
                publicKey = peerEphemeral;

                // Use the long-term key of the Responder as private key
                privateKey = session.getKeyPair();
            }

            // Consistency check of key type and curve against the selected cipher suite
            int selectedCipherSuite = session.getSelectedCipherSuite();

            if (!Util.checkDiffieHellmanKeyAgainstCipherSuite(privateKey, selectedCipherSuite)) {
                LOGGER.debug("ERROR: Computing the Diffie-Hellman Secret (privateKey check)");
                return null;
            }

            if (!Util.checkDiffieHellmanKeyAgainstCipherSuite(publicKey, selectedCipherSuite)) {
                LOGGER.debug("ERROR: Computing the Diffie-Hellman Secret (publicKey check)");
                return null;
            }

            dhSecret = SharedSecretCalculation.generateSharedSecret(privateKey, publicKey);

            if (dhSecret == null) {
                LOGGER.debug("ERROR: Computing the Diffie-Hellman Secret (generation)");
                return null;
            }

            LOGGER.debug(byteArrayToString("G_RX", dhSecret));

            String hashAlgorithm = EdhocSession.getEdhocHashAlg(selectedCipherSuite);

            // Compute SALT_3e2m
            byte[] salt3e2m;
            int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
            CBORObject context = CBORObject.FromObject(th2);

            try {
                salt3e2m = session.edhocKDF(prk2e, Constants.KDF_LABEL_SALT_3E2M, context, length);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                System.err.println("ERROR: Generating SALT_3e2m\n" + e.getMessage());
                return null;
            }

            LOGGER.debug(byteArrayToString("SALT_3e2m", salt3e2m));

            if (hashAlgorithm.equals("SHA-256") || hashAlgorithm.equals("SHA-384") || hashAlgorithm.equals("SHA-512")) {
                try {
                    prk3e2m = Hkdf.extract(salt3e2m, dhSecret);
                } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                    System.err.println("ERROR: Generating PRK_3e2m\n" + e.getMessage());
                    return null;
                }
            }
        }
        return prk3e2m;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeMAC2} */
    protected byte[] computeMAC2(EdhocSession session, byte[] prk3e2m, byte[] th2,
                                 CBORObject idCredR, byte[] credR, CBORObject[] ead2) {

        // Build the CBOR sequence to use for 'context': ( ID_CRED_R, TH_2, CRED_R, ? EAD_2 )
        // The actual 'context' is a CBOR byte string with value the serialization of the CBOR sequence
        List<CBORObject> objectList = new ArrayList<>();
        objectList.add(idCredR);
        objectList.add(CBORObject.FromObject(th2));
        objectList.add(CBORObject.DecodeFromBytes(credR));

        if (ead2 != null) {
            Collections.addAll(objectList, ead2);
        }

        byte[] contextSequence = Util.buildCBORSequence(objectList);
        CBORObject context = CBORObject.FromObject(contextSequence);
        LOGGER.debug(byteArrayToString( "context_2", contextSequence));

        int macLength = 0;
        int method = session.getMethod();
        int selectedCipherSuite = session.getSelectedCipherSuite();
        if (method == 0 || method == 2) {
            macLength = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
        }
        if (method == 1 || method == 3) {
            macLength = EdhocSession.getTagLengthEdhocAEAD(selectedCipherSuite);
        }

        try {
            return session.edhocKDF(prk3e2m, Constants.KDF_LABEL_MAC_2, context, macLength);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Computing MAC_2\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeExternalData} */
    protected byte[] computeExternalData(byte[] th, byte[] cred, CBORObject[] ead) {

        if (th == null || cred == null)
            return null;

        List<CBORObject> externalDataList = new ArrayList<>();

        // TH2 / TH3 is the first element of the CBOR Sequence
        byte[] thSerializedCBOR = CBORObject.FromObject(th).EncodeToBytes();
        externalDataList.add(CBORObject.FromObject(thSerializedCBOR));

        // CRED_R / CRED_I is the second element of the CBOR Sequence
        externalDataList.add(CBORObject.FromObject(cred));

        // EAD_2 / EAD_3 is the third element of the CBOR Sequence (if provided)
        if (ead != null) {
            List<CBORObject> objectList = new ArrayList<>();

            Collections.addAll(objectList, ead);

            // Rebuild how EAD was in the EDHOC message
            byte[] eadSequence = Util.buildCBORSequence(objectList);

            externalDataList.add(CBORObject.FromObject(eadSequence));
        }

        return Util.concatenateByteArrays(externalDataList);
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeSignatureOrMac2} */
    protected byte[] computeSignatureOrMac2(EdhocSession session, byte[] mac2, byte[] externalData) {
        // Used by Responder
        byte[] signatureOrMac2 = null;
        int authenticationMethod = session.getMethod();

        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_1
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The responder does not use signatures as authentication method,
            // then Signature_or_MAC_2 is equal to MAC_2
            signatureOrMac2 = new byte[mac2.length];
            System.arraycopy(mac2, 0, signatureOrMac2, 0, mac2.length);
        } else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_2) {
            // The responder uses signatures as authentication method,
            // then Signature_or_MAC_2 is signature that has to be computed
            try {
                OneKey identityKey = session.getKeyPair();
                int selectedCipherSuite = session.getSelectedCipherSuite();

                // Consistency check of key type and curve against the selected cipher suite
                if (!Util.checkSignatureKeyAgainstCipherSuite(identityKey, selectedCipherSuite)) {
                    LOGGER.debug("ERROR: Signing MAC_2 to produce Signature_or_MAC_2 (signature key check)");
                    return null;
                }

                LOGGER.debug(byteArrayToString("External Data for signing MAC_2 to produce Signature_or_MAC_2",
                        externalData));

                signatureOrMac2 = Util.computeSignature(session.getIdCred(), externalData, mac2, identityKey);

            } catch (CoseException e) {
                LOGGER.debug("ERROR: Signing MAC_2 to produce Signature_or_MAC_2\n" + e.getMessage());
                return null;
            }
        }

        return signatureOrMac2;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeKeystream2} */
    protected byte[] computeKeystream2(EdhocSession session, byte[] th2, byte[] prk2e, int length) {
        CBORObject context = CBORObject.FromObject(th2);

        try {
            return session.edhocKDF(prk2e, Constants.KDF_LABEL_KEYSTREAM_2, context, length);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Generating KEYSTREAM_2\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#verifySignatureOrMac2} */
    protected boolean verifySignatureOrMac2(EdhocSession session, OneKey peerLongTerm, CBORObject peerIdCred,
                                            byte[] signatureOrMac2, byte[] externalData, byte[] mac2) {
        // Used by Initiator
        int authenticationMethod = session.getMethod();

        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_1
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The responder does not use signatures as authentication method,
            // then Signature_or_MAC_2 has to be equal to MAC_2
            return Arrays.equals(signatureOrMac2, mac2);
        } else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_2) {
            // The responder uses signatures as authentication method,
            // then Signature_or_MAC_2 is a signature to verify
            int selectedCipherSuite = session.getSelectedCipherSuite();

            // Consistency check of key type and curve against the selected cipher suite
            if (!Util.checkSignatureKeyAgainstCipherSuite(peerLongTerm, selectedCipherSuite)) {
                LOGGER.debug("ERROR: Verifying the signature of Signature_or_MAC_2 (signature check)");
                return false;
            }

            try {
                return Util.verifySignature(signatureOrMac2, peerIdCred, externalData, mac2, peerLongTerm);
            } catch (CoseException e) {
                LOGGER.debug("ERROR: Verifying the signature of Signature_or_MAC_2\n" + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeTH3} */
    protected byte[] computeTH3(String hashAlgorithm, byte[] th2, byte[] plaintext2) {
        int inputLength = th2.length + plaintext2.length;
        int offset = 0;
        byte[] hashInput = new byte[inputLength];
        System.arraycopy(th2, 0, hashInput, offset, th2.length);
        offset += th2.length;
        System.arraycopy(plaintext2, 0, hashInput, offset, plaintext2.length);

        try {
            return Util.computeHash(hashInput, hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Invalid hash algorithm when computing TH3\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computePRK4e3m} */
    protected byte[] computePRK4e3m(EdhocSession session, byte[] prk3e2m, byte[] th3, OneKey peerLongTerm,
                                        OneKey peerEphemeral) {
        byte[] prk4e3m = null;
        int authenticationMethod = session.getMethod();

        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_1) {
            // The initiator uses signatures as authentication method,
            // then PRK_4e3m is equal to PRK_3e2m
            prk4e3m = new byte[prk3e2m.length];
            System.arraycopy(prk3e2m, 0, prk4e3m, 0, prk3e2m.length);
        } else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_2
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The initiator does not use signatures as authentication method,
            // then PRK_4e3m has to be computed
            byte[] dhSecret;
            OneKey privateKey;
            OneKey publicKey;

            if (session.isInitiator()) {
                // Use the ephemeral key of the Responder as public key
                publicKey = peerEphemeral;

                // Use the long-term key of the Initiator as private key
                privateKey = session.getKeyPair();
            } else {
                // Use the ephemeral key of the Responder as private key
                privateKey = session.getEphemeralKey();

                // Use the long-term key of the Initiator as public key
                publicKey = peerLongTerm;
            }

            // Consistency check of key type and curve against the selected cipher suite
            int selectedCipherSuite = session.getSelectedCipherSuite();

            if (!Util.checkDiffieHellmanKeyAgainstCipherSuite(privateKey, selectedCipherSuite)) {
                LOGGER.debug("ERROR: Computing the Diffie-Hellman Secret (privateKey check)");
                return null;
            }

            if (!Util.checkDiffieHellmanKeyAgainstCipherSuite(publicKey, selectedCipherSuite)) {
                LOGGER.debug("ERROR: Computing the Diffie-Hellman Secret (publicKey check)");
                return null;
            }

            dhSecret = SharedSecretCalculation.generateSharedSecret(privateKey, publicKey);

            if (dhSecret == null) {
                LOGGER.debug("ERROR: Computing the Diffie-Hellman Secret");
                return null;
            }

            LOGGER.debug(byteArrayToString("G_IY", dhSecret));

            String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());

            // Compute SALT_4e3m
            byte[] salt4e3m;
            int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
            CBORObject context = CBORObject.FromObject(th3);

            try {
                salt4e3m = session.edhocKDF(prk3e2m, Constants.KDF_LABEL_SALT_4E3M, context, length);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                LOGGER.debug("ERROR: Generating SALT_4e3m\n" + e.getMessage());
                return null;
            }

            if (salt4e3m == null) {
                LOGGER.debug("ERROR: Computing SALT_4e3m");
                return null;
            }

            LOGGER.debug(byteArrayToString("SALT_4e3m", salt4e3m));

            if (hashAlgorithm.equals("SHA-256") || hashAlgorithm.equals("SHA-384") || hashAlgorithm.equals("SHA-512")) {
                try {
                    prk4e3m = Hkdf.extract(salt4e3m, dhSecret);
                } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                    System.err.println("ERROR: Generating PRK_4e3m\n" + e.getMessage());
                    return null;
                }
            }
        }

        return prk4e3m;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeMAC3} */
    protected byte[] computeMAC3(EdhocSession session, byte[] prk4e3m, byte[] th3, CBORObject idCredI,
                                 byte[] credI, CBORObject[] ead3) {

        // Build the CBOR sequence for 'context': ( ID_CRED_I, TH_3, CRED_I, ? EAD_3 )
        // The actual 'context' is a CBOR byte string with value the serialization of the CBOR sequence
        List<CBORObject> objectList = new ArrayList<>();
        objectList.add(idCredI);
        objectList.add(CBORObject.FromObject(th3)); // v-14
        objectList.add(CBORObject.DecodeFromBytes(credI));

        if (ead3 != null) {
            Collections.addAll(objectList, ead3);
        }
        byte[] contextSequence = Util.buildCBORSequence(objectList);
        CBORObject context = CBORObject.FromObject(contextSequence);
        LOGGER.debug(byteArrayToString("context_3", contextSequence));

        int macLength = 0;
        int method = session.getMethod();
        int selectedCipherSuite = session.getSelectedCipherSuite();
        if (method == Constants.EDHOC_AUTH_METHOD_0 || method == Constants.EDHOC_AUTH_METHOD_1) {
            macLength = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
        }
        if (method == Constants.EDHOC_AUTH_METHOD_2 || method == Constants.EDHOC_AUTH_METHOD_3) {
            macLength = EdhocSession.getTagLengthEdhocAEAD(selectedCipherSuite);
        }

        try {
            return session.edhocKDF(prk4e3m, Constants.KDF_LABEL_MAC_3, context, macLength);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Computing MAC_3\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeSignatureOrMac3} */
    protected byte[] computeSignatureOrMac3(EdhocSession session, byte[] mac3, byte[] externalData) {
        // Used by Initiator
        byte[] signatureOrMac3 = null;
        int authenticationMethod = session.getMethod();

        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_2
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The initiator does not use signatures as authentication method,
            // then Signature_or_MAC_3 is equal to MAC_3
            signatureOrMac3 = new byte[mac3.length];
            System.arraycopy(mac3, 0, signatureOrMac3, 0, mac3.length);
        } else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_1) {
            // The initiator uses signatures as authentication method,
            // then Signature_or_MAC_3 is signature that has to be computed
            try {
                OneKey identityKey = session.getKeyPair();
                int selectedCipherSuite = session.getSelectedCipherSuite();

                // Consistency check of key type and curve against the selected cipher suite
                if (!Util.checkSignatureKeyAgainstCipherSuite(identityKey, selectedCipherSuite)) {
                    LOGGER.debug("ERROR: Signing MAC_3 to produce Signature_or_MAC_3 (signature key check)");
                    return null;
                }

                LOGGER.debug(byteArrayToString( "External Data for signing MAC_3 to produce Signature_or_MAC_3",
                        externalData));

                signatureOrMac3 = Util.computeSignature(session.getIdCred(), externalData, mac3, identityKey);

            } catch (CoseException e) {
                LOGGER.debug("ERROR: Signing MAC_3 to produce Signature_or_MAC_3\n" + e.getMessage());
                return null;
            }
        }

        return signatureOrMac3;

    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey}
     * and {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey} */
    protected byte[] computeKeyOrIV3(String keyName, EdhocSession session, byte[] th3, byte[] prk3e2m) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        CBORObject context = CBORObject.FromObject(th3);

        String name;
        int length;
        int label;

        switch(keyName) {
            case "KEY" -> {
                name = "K_3";
                length = EdhocSession.getKeyLengthEdhocAEAD(selectedCipherSuite);
                label = Constants.KDF_LABEL_K_3;
            }
            case "IV" -> {
                name = "IV_3";
                length = EdhocSession.getIvLengthEdhocAEAD(selectedCipherSuite);
                label = Constants.KDF_LABEL_IV_3;
            }
            default -> {
                return null;
            }
        }

        if (length == 0) {
            return null;
        }

        try {
            return session.edhocKDF(prk3e2m, label, context, length);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Generating {}\n" + e.getMessage(), name);
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeCiphertext3} */
    protected byte[] computeCiphertext3(int selectedCipherSuite, byte[] externalData, byte[] plaintext, byte[] k3ae,
                                        byte[] iv3ae) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return Util.encrypt(emptyMap, externalData, plaintext, alg, iv3ae, k3ae);
        } catch (CoseException e) {
            LOGGER.debug("ERROR: Computing CIPHERTEXT_3\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeTH4} */
    protected byte[] computeTH4(String hashAlgorithm, byte[] th3, byte[] plaintext3) {
        int inputLength = th3.length + plaintext3.length;
        byte[] hashInput = new byte[inputLength];
        System.arraycopy(th3, 0, hashInput, 0, th3.length);
        System.arraycopy(plaintext3, 0, hashInput, th3.length, plaintext3.length);
        try {
            return Util.computeHash(hashInput, hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Invalid hash algorithm when computing TH4\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computePRKout} */
    protected byte[] computePRKout(EdhocSession session, byte[] th4, byte[] prk4e3m) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
        CBORObject context = CBORObject.FromObject(th4);

        try {
            return session.edhocKDF(prk4e3m, Constants.KDF_LABEL_PRK_OUT, context, length);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Generating PRK_out\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computePRKexporter} */
    protected byte[] computePRKexporter(EdhocSession session, byte[] prkOut) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
        CBORObject context = CBORObject.FromObject(new byte[0]);

        try {
            return session.edhocKDF(prkOut, Constants.KDF_LABEL_PRK_EXPORTER, context, length);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Generating PRK_exporter\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#decryptCiphertext3} */
    protected byte[] decryptCiphertext3(int selectedCipherSuite, byte[] externalData, byte[] ciphertext, byte[] k3ae,
                                        byte[] iv3ae) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return Util.decrypt(emptyMap, externalData, ciphertext, alg, iv3ae, k3ae);
        } catch (CoseException e) {
            LOGGER.debug("ERROR: Decrypting CIPHERTEXT_3\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#verifySignatureOrMac3} */
    protected boolean verifySignatureOrMac3(EdhocSession session, OneKey peerLongTerm, CBORObject peerIdCred,
                                            byte[] signatureOrMac3, byte[] externalData, byte[] mac3) {
        // Used by Responder
        int authenticationMethod = session.getMethod();

        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_2
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The initiator does not use signatures as authentication method,
            // then Signature_or_MAC_3 has to be equal to MAC_3
            return Arrays.equals(signatureOrMac3, mac3);
        }
        else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_1) {
            // The initiator uses signatures as authentication method,
            // then Signature_or_MAC_3 is a signature to verify
            int selectedCipherSuite = session.getSelectedCipherSuite();

            // Consistency check of key type and curve against the selected cipher suite
            if (!Util.checkSignatureKeyAgainstCipherSuite(peerLongTerm, selectedCipherSuite)) {
                LOGGER.debug("ERROR: Verifying the signature of Signature_or_MAC_3");
                return false;
            }

            try {
                return Util.verifySignature(signatureOrMac3, peerIdCred, externalData, mac3, peerLongTerm);
            } catch (CoseException e) {
                LOGGER.debug("ERROR: Verifying the signature of Signature_or_MAC_3\n" + e.getMessage());
                return false;
            }
        }

        return false;
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey}
     * and {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey} */
    protected byte[] computeKeyOrIV4(String keyName, EdhocSession session, byte[] th4, byte[] prk4e3m) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        CBORObject context = CBORObject.FromObject(th4);

        String name;
        int length;
        int label;

        switch(keyName) {
            case "KEY" -> {
                name = "K_4";
                length = EdhocSession.getKeyLengthEdhocAEAD(selectedCipherSuite);
                label = Constants.KDF_LABEL_K_4;
            }
            case "IV" -> {
                name = "IV_4";
                length = EdhocSession.getIvLengthEdhocAEAD(selectedCipherSuite);
                label = Constants.KDF_LABEL_IV_4;
            }
            default -> {
                return null;
            }
        }

        if (length == 0) {
            return null;
        }

        try {
            return session.edhocKDF(prk4e3m, label, context, length);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.debug("ERROR: Generating {}\n" + e.getMessage(), name);
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#computeCiphertext4} */
    protected byte[] computeCiphertext4(int selectedCipherSuite, byte[] externalData, byte[] plaintext, byte[] k4m,
                                        byte[] iv4m) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return Util.encrypt(emptyMap, externalData, plaintext, alg, iv4m, k4m);
        } catch (CoseException e) {
            LOGGER.debug("ERROR: Computing CIPHERTEXT_4\n" + e.getMessage());
            return null;
        }
    }

    /** Adapted from {@link org.eclipse.californium.edhoc.MessageProcessor#decryptCiphertext4} */
    protected byte[] decryptCiphertext4(int selectedCipherSuite, byte[] externalData, byte[] ciphertext,
                                            byte[] k4ae, byte[] iv4ae) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return Util.decrypt(emptyMap, externalData, ciphertext, alg, iv4ae, k4ae);
        } catch (CoseException e) {
            LOGGER.debug("ERROR: Decrypting CIPHERTEXT_4\n" + e.getMessage());
            return null;
        }
    }

}