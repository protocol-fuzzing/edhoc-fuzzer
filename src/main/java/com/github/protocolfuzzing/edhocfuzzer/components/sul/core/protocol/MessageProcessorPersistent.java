package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;
import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.cose.*;
import org.eclipse.californium.edhoc.*;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Taken and heavily adapted from
 * {@link org.eclipse.californium.edhoc.MessageProcessor}.
 * Substitutes the static functions of
 * {@link org.eclipse.californium.edhoc.MessageProcessor} mainly by removing
 * field cleanup and purge session calls and by delaying changes to session. The
 * read functions return boolean.
 * These functions are no longer static and take their parameters also from the
 * class's field edhocMapperState.
 */
public class MessageProcessorPersistent {
    private static final Logger LOGGER = LogManager.getLogger();
    protected EdhocMapperState edhocMapperState;

    public MessageProcessorPersistent(EdhocMapperState edhocMapperState) {
        this.edhocMapperState = edhocMapperState;
    }

    public EdhocMapperState getEdhocMapperState() {
        return edhocMapperState;
    }

    public enum StructureCodes {
        EDHOC_MESSAGE_1,
        EDHOC_MESSAGE_2,
        EDHOC_MESSAGE_2_OR_3_OR_4,
        EDHOC_MESSAGE_3_OR_4,
        EDHOC_ERROR_MESSAGE,
        UNKNOWN_MESSAGE
    }

    /**
     * Tries to match the byte sequence's structure of CBOR elements with an edhoc
     * message
     */
    public StructureCodes messageTypeFromStructure(byte[] sequence) {
        LOGGER.debug("Start of messageTypeFromStructure");
        if (sequence == null) {
            return StructureCodes.UNKNOWN_MESSAGE;
        }

        CBORObject[] elements;
        try {
            elements = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (CBORException e) {
            LOGGER.error("MessageType: " + e.getMessage());
            return StructureCodes.UNKNOWN_MESSAGE;
        }

        // Error Message has 2 elements and possible prepended CX,
        // but is distinguishable from message 2 which has the same structure
        if (hasErrorMessageStructure(elements)) {
            return StructureCodes.EDHOC_ERROR_MESSAGE;
        }

        // A CoAP client receives responses from CoAP server without connection
        // identifiers prepended
        // A CoAP server receives requests from CoAP clients with C_I or C_R prepended
        // if enabled
        int cX_offset = edhocMapperState.receiveWithPrependedCX() ? 1 : 0;
        int messageElementsLength = elements.length - cX_offset;

        switch (messageElementsLength) {
            case 4, 5 -> {
                // message 1 has 4 or 5 elements with EAD_1
                return StructureCodes.EDHOC_MESSAGE_1;
            }
            case 2 -> {
                if (hasProtocolVersionLeqV19()) {
                    // message 2 has 2 elements for <=v19
                    return StructureCodes.EDHOC_MESSAGE_2;
                }

                return StructureCodes.UNKNOWN_MESSAGE;
            }
            case 1 -> {
                if (hasProtocolVersionLeqV19()) {
                    // message 3 and 4 have 1 element for <=v19
                    return StructureCodes.EDHOC_MESSAGE_3_OR_4;
                }

                // message 2, 3 and 4 have 1 element
                return StructureCodes.EDHOC_MESSAGE_2_OR_3_OR_4;
            }
            default -> {
                return StructureCodes.UNKNOWN_MESSAGE;
            }
        }
    }

    /* Initiator message functions -- only session of Initiator is modified */

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage1}
     */
    public byte[] writeMessage1() {
        LOGGER.debug("Start of writeMessage1");
        EdhocSessionPersistent session = edhocMapperState.getEdhocSessionPersistent();

        // Prepare the list of CBOR objects to build the CBOR sequence
        List<CBORObject> objectList = new ArrayList<>();

        // if EDHOC message_1 is transported in a CoAP request
        // CBOR simple value 'true' must be prepended if enabled
        if (edhocMapperState.sendWithPrependedCX()) {
            objectList.add(CBORObject.True);
        }

        // METHOD as CBOR integer
        int method = session.getMethod();
        CBORObject method_cbor = CBORObject.FromObject(method);
        LOGGER.debug(EdhocUtil.byteArrayToString("METHOD", method_cbor.EncodeToBytes()));
        objectList.add(method_cbor);

        // SUITES_I as CBOR integer or CBOR array
        List<Integer> supportedCipherSuites = session.getSupportedCipherSuites();
        List<Integer> peerSupportedCipherSuites = session.getPeerSupportedCipherSuites();

        int selectedSuite = -1;
        int preferredSuite = supportedCipherSuites.get(0);

        if (peerSupportedCipherSuites == null || peerSupportedCipherSuites.isEmpty()) {
            // No SUITES_R has been received, so it is not known what cipher suites the
            // responder supports
            // The selected cipher suite is the most preferred by the initiator
            selectedSuite = preferredSuite;
        } else {
            // SUITES_R has been received, so it is known what cipher suites the responder
            // supports
            // Pick the selected cipher suite as the most preferred by the Initiator from
            // the ones
            // supported by the Responder
            for (Integer i : supportedCipherSuites) {
                if (peerSupportedCipherSuites.contains(i)) {
                    selectedSuite = i;
                    break;
                }
            }
        }

        if (selectedSuite == -1) {
            LOGGER.error("W_M1: Impossible to agree on a mutually supported cipher suite");
            return null;
        }

        // Set the selected cipher suite
        session.setSelectedCipherSuite(selectedSuite);

        // Set the asymmetric key pair, CRED and ID_CRED of the Initiator to use in this
        // session
        session.setAuthenticationCredential();

        // Set the ephemeral keys of the Initiator to use in this session
        if (session.getEphemeralKey() == null) {
            session.setEphemeralKey();
        }

        CBORObject suitesI;
        if (selectedSuite == preferredSuite) {
            // SUITES_I is only the selected suite, as a CBOR integer
            suitesI = CBORObject.FromObject(selectedSuite);
        } else {
            // SUITES_I is a CBOR array
            // The elements are the Initiator's supported cipher suite in decreasing order
            // of preference,
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
        LOGGER.debug(EdhocUtil.byteArrayToString("SUITES_I", suitesI.EncodeToBytes()));
        objectList.add(suitesI);

        // G_X as a CBOR byte string
        CBORObject gX = switch (selectedSuite) {
            case Constants.EDHOC_CIPHER_SUITE_0, Constants.EDHOC_CIPHER_SUITE_1 ->
                session.getEphemeralKey().PublicKey().get(KeyKeys.OKP_X);
            case Constants.EDHOC_CIPHER_SUITE_2, Constants.EDHOC_CIPHER_SUITE_3 ->
                session.getEphemeralKey().PublicKey().get(KeyKeys.EC2_X);
            default ->
                null;
        };

        if (gX == null) {
            LOGGER.error("W_M1: Invalid G_X");
            return null;
        }

        objectList.add(gX);
        LOGGER.debug(EdhocUtil.byteArrayToString("G_X", gX.GetByteString()));

        // C_I
        byte[] connectionIdentifierInitiator = session.getConnectionId();
        CBORObject cI = encodeIdentifier(connectionIdentifierInitiator);
        LOGGER.debug(
                EdhocUtil.byteArrayToString("Connection Identifier of the Initiator", connectionIdentifierInitiator));
        LOGGER.debug(EdhocUtil.byteArrayToString("C_I", cI.EncodeToBytes()));
        objectList.add(cI);

        // Produce possible EAD items following early instructions from the application
        SideProcessor sideProcessor = session.getSideProcessor();

        sideProcessor.produceIndependentEADs(Constants.EDHOC_MESSAGE_1);

        // An error occurred
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_1, false)
                .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

            String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_1, false)
                    .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                    .get(0)
                    .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                    .AsString();

            // No need to keep this information any longer in the side processor object
            sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_1, Constants.SIDE_PROCESSOR_OUTER_ERROR, false);

            LOGGER.error("W_M1: Using side processor on message 1: {}", error);
            return null;
        }

        List<CBORObject> ead1List = sideProcessor.getProducedEADs(Constants.EDHOC_MESSAGE_1);

        // EAD_1, if provided
        if (ead1List != null && !ead1List.isEmpty()) {
            objectList.addAll(ead1List);
        }

        /* Prepare EDHOC Message 1 */
        byte[] message1 = EdhocUtil.buildCBORSequence(objectList);
        LOGGER.debug(EdhocUtil.byteArrayToString("EDHOC Message 1", message1));

        // Compute and store the hash of Message 1
        // In case of CoAP request the first byte 0xf5 must be skipped
        int offset = edhocMapperState.sendWithPrependedCX() ? 1 : 0;
        byte[] hashMessage1 = new byte[message1.length - offset];
        System.arraycopy(message1, offset, hashMessage1, 0, hashMessage1.length);

        /* Modify session */
        if (session.isInitiator()) {
            session.setHashMessage1(hashMessage1);
        }

        return message1;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage2}
     */
    public boolean readMessage2(byte[] sequence) {
        LOGGER.debug("Start of readMessage2");
        Map<CBORObject, EdhocSessionPersistent> edhocSessions = edhocMapperState.getEdhocEndpointInfoPersistent()
                .getEdhocSessionsPersistent();
        Map<CBORObject, OneKey> peerPublicKeys = edhocMapperState.getEdhocEndpointInfoPersistent().getPeerPublicKeys();
        Map<CBORObject, CBORObject> peerCredentials = edhocMapperState.getEdhocEndpointInfoPersistent()
                .getPeerCredentials();
        Set<CBORObject> usedConnectionIds = edhocMapperState.getEdhocEndpointInfoPersistent().getUsedConnectionIds();
        Set<CBORObject> ownIdCreds = edhocMapperState.getOwnIdCreds();

        if (sequence == null || edhocSessions == null || peerPublicKeys == null || peerCredentials == null
                || usedConnectionIds == null) {
            LOGGER.error("R_M2: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.error("R_M2: Unable to decode byte sequence to CBOR object array");
            return false;
        }

        /* Consistency checks */

        // C_I
        byte[] connectionIdentifierInitiator;
        if (edhocMapperState.receiveWithPrependedCX()) {
            // CoAP Server as Initiator
            // Message 2 is transported in a CoAP request
            // C_I is present as first element of the CBOR sequence
            index++;
            CBORObject cI = objectListRequest[index];

            if (cI.getType() != CBORType.ByteString && cI.getType() != CBORType.Integer) {
                LOGGER.error("R_M2: C_I must be a byte string or an integer");
                return false;
            }

            connectionIdentifierInitiator = decodeIdentifier(cI);

            if (connectionIdentifierInitiator == null) {
                LOGGER.error("R_M2: Invalid encoding of C_I");
                return false;
            }
        } else {
            // CoAP Client as Initiator when Message 2 is a CoAP response of a previous
            // Message 1 request
            // or CoAP Server as Initiator with correlation with CX is disabled
            connectionIdentifierInitiator = edhocMapperState.getEdhocSessionPersistent().getConnectionId();
        }

        LOGGER.debug(
                EdhocUtil.byteArrayToString("Connection Identifier of the Initiator", connectionIdentifierInitiator));

        CBORObject connectionIdentifierInitiatorCbor = CBORObject.FromObject(connectionIdentifierInitiator);
        EdhocSessionPersistent session = edhocSessions.get(connectionIdentifierInitiatorCbor);
        if (session == null) {
            LOGGER.error("R_M2: EDHOC session not found");
            return false;
        }

        // G_Y | CIPHERTEXT_2
        index++;

        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.error("R_M2: (G_Y | CIPHERTEXT_2) must be a byte string");
            return false;
        }

        byte[] gY_Ciphertext2 = objectListRequest[index].GetByteString();

        int gYLength = EdhocSession.getEphermeralKeyLength(session.getSelectedCipherSuite());
        int ciphertext2Length = gY_Ciphertext2.length - gYLength;

        if (ciphertext2Length <= 0) {
            LOGGER.error("R_M2: CIPHERTEXT_2 has non-positive size");
            return false;
        }

        // G_Y
        byte[] gY = new byte[gYLength];
        System.arraycopy(gY_Ciphertext2, 0, gY, 0, gYLength);

        LOGGER.debug(EdhocUtil.byteArrayToString("G_Y", gY));

        // Ephemeral public key of the Responder
        int selectedCipherSuite = session.getSelectedCipherSuite();

        OneKey peerEphemeralKey = switch (selectedCipherSuite) {
            case Constants.EDHOC_CIPHER_SUITE_0, Constants.EDHOC_CIPHER_SUITE_1 ->
                SharedSecretCalculation.buildCurve25519OneKey(null, gY);
            case Constants.EDHOC_CIPHER_SUITE_2, Constants.EDHOC_CIPHER_SUITE_3 ->
                SharedSecretCalculation.buildEcdsa256OneKey(null, gY, null);
            default ->
                null;
        };

        if (peerEphemeralKey == null) {
            LOGGER.error("R_M2: Invalid ephemeral public key G_Y");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("PeerEphemeralPublicKey", peerEphemeralKey.AsCBOR().EncodeToBytes()));

        // CIPHERTEXT_2
        byte[] ciphertext2 = new byte[ciphertext2Length];
        System.arraycopy(gY_Ciphertext2, gYLength, ciphertext2, 0, ciphertext2Length);
        LOGGER.debug(EdhocUtil.byteArrayToString("CIPHERTEXT_2", ciphertext2));

        CBORObject cR = null;
        byte[] connectionIdentifierResponder = null;

        if (hasProtocolVersionLeqV19()) {
            // C_R for version leq v19
            index++;
            cR = objectListRequest[index];

            if (cR.getType() != CBORType.ByteString && cR.getType() != CBORType.Integer) {
                LOGGER.error("R_M2: C_R must be a byte string or an integer");
                return false;
            }

            connectionIdentifierResponder = decodeIdentifier(cR);
            if (connectionIdentifierResponder == null) {
                LOGGER.error("R_M2: Invalid encoding of C_R");
                return false;
            }

            LOGGER.debug(EdhocUtil.byteArrayToString("Connection Identifier of the Responder",
                    connectionIdentifierResponder));
            LOGGER.debug(EdhocUtil.byteArrayToString("C_R", cR.EncodeToBytes()));

            if (session.getApplicationProfile().getUsedForOSCORE()
                    && Arrays.equals(connectionIdentifierInitiator, connectionIdentifierResponder)) {
                LOGGER.warn("R_M2: Found C_R equal to C_I in an OSCORE enabled Application Profile");
            }
        }

        /* Decrypt CIPHERTEXT_2 */

        // Compute TH2
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());
        byte[] hashMessage1 = session.getHashMessage1();
        byte[] hashMessage1SerializedCBOR = CBORObject.FromObject(hashMessage1).EncodeToBytes();
        byte[] gYSerializedCBOR = CBORObject.FromObject(gY).EncodeToBytes();

        byte[] th2 = null;
        if (hasProtocolVersionLeqV19()) {
            th2 = computeTH2(hashAlgorithm, gYSerializedCBOR, cR.EncodeToBytes(), hashMessage1SerializedCBOR);
        } else {
            th2 = computeTH2(hashAlgorithm, gYSerializedCBOR, new byte[0], hashMessage1SerializedCBOR);
        }

        if (th2 == null) {
            LOGGER.error("R_M2: Computing TH2");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("H(message_1)", hashMessage1));
        LOGGER.debug(EdhocUtil.byteArrayToString("TH_2", th2));

        // Compute the Diffie-Hellman secret G_XY
        byte[] dhSecret = SharedSecretCalculation.generateSharedSecret(session.getEphemeralKey(), peerEphemeralKey);

        if (dhSecret == null) {
            LOGGER.error("R_M2: Computing the Diffie-Hellman secret G_XY");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("G_XY", dhSecret));

        // Compute PRK_2e
        byte[] prk2e = null;
        if (hasProtocolVersionLeqV15()) {
            prk2e = computePRK2e(new byte[0], dhSecret, hashAlgorithm);
        } else {
            prk2e = computePRK2e(th2, dhSecret, hashAlgorithm);
        }

        if (prk2e == null) {
            LOGGER.error("R_M2: Computing PRK_2e");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_2e", prk2e));

        // Compute KEYSTREAM_2
        byte[] keystream2 = computeKeystream2(session, th2, prk2e, ciphertext2.length);
        if (keystream2 == null) {
            LOGGER.error("R_M2: Computing KEYSTREAM_2");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("KEYSTREAM_2", keystream2));

        // Compute the plaintext
        byte[] plaintext2 = EdhocUtil.arrayXor(ciphertext2, keystream2);
        LOGGER.debug(EdhocUtil.byteArrayToString("Plaintext retrieved from CIPHERTEXT_2", plaintext2));

        // Parse the plaintext as a CBOR sequence
        // baseIndex is the index of ID_CRED_R
        int baseIndex = 0;
        CBORObject[] plaintextElementList;
        try {
            plaintextElementList = CBORObject.DecodeSequenceFromBytes(plaintext2);
        } catch (Exception e) {
            LOGGER.error("R_M2: Malformed or invalid CBOR encoded plaintext from CIPHERTEXT_2");
            return false;
        }

        if (plaintextElementList.length == 0) {
            LOGGER.error("R_M2: Zero-length plaintext_2");
            return false;
        }

        if (hasProtocolVersionLeqV17()) {
            // Discard possible padding prepended to the plaintext
            while (baseIndex < plaintextElementList.length
                    && plaintextElementList[baseIndex].equals(CBORObject.True)) {
                baseIndex++;
            }
        }

        if (hasProtocolVersionLeqV19()) {
            // ID_CRED_R and Signature_or_MAC_2 should be contained
            if (plaintextElementList.length - baseIndex < 2) {
                LOGGER.error("R_M2: Plaintext_2 contains less than two elements");
                return false;
            }
        } else {
            // C_R, ID_CRED_R and Signature_or_MAC_2 should be contained
            if (plaintextElementList.length - baseIndex < 3) {
                LOGGER.error("R_M2: Plaintext_2 contains less than three elements");
                return false;
            }

            // C_R for version greater than v19
            cR = plaintextElementList[baseIndex];
            baseIndex++;

            if (cR.getType() != CBORType.ByteString && cR.getType() != CBORType.Integer) {
                LOGGER.error("R_M2: C_R must be a byte string or an integer");
                return false;
            }

            connectionIdentifierResponder = decodeIdentifier(cR);
            if (connectionIdentifierResponder == null) {
                LOGGER.error("R_M2: Invalid encoding of C_R");
                return false;
            }

            LOGGER.debug(EdhocUtil.byteArrayToString("Connection Identifier of the Responder",
                    connectionIdentifierResponder));
            LOGGER.debug(EdhocUtil.byteArrayToString("C_R", cR.EncodeToBytes()));

            if (session.getApplicationProfile().getUsedForOSCORE()
                    && Arrays.equals(connectionIdentifierInitiator, connectionIdentifierResponder)) {
                LOGGER.warn("R_M2: Found C_R equal to C_I in an OSCORE enabled Application Profile");
            }
        }

        // check ID_CRED_R
        if (plaintextElementList[baseIndex].getType() != CBORType.ByteString
                && plaintextElementList[baseIndex].getType() != CBORType.Integer
                && plaintextElementList[baseIndex].getType() != CBORType.Map) {
            LOGGER.error("R_M2: Invalid type of ID_CRED_R in plaintext_2");
            return false;
        }

        // check Signature_or_MAC_2
        if (plaintextElementList[baseIndex + 1].getType() != CBORType.ByteString) {
            LOGGER.error("R_M2: Signature_or_MAC_2 must be a byte string");
            return false;
        }

        // check EAD_2
        CBORObject[] ead2 = null;
        int length = plaintextElementList.length - baseIndex - 2;
        if (length > 0) {
            // EAD_2 is present
            if (hasProtocolVersionLeqV17()) {
                ead2 = preParseEADleqV17(plaintextElementList, baseIndex + 2, session.getSupportedEADs());
            } else {
                ead2 = preParseEAD(plaintextElementList, baseIndex + 2, 2, session.getSupportedEADs());
            }

            if (ead2 == null) {
                LOGGER.error("R_M2: Processing EAD_2");
                return false;
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
            LOGGER.error("R_M2: Invalid format for ID_CRED_R");
            return false;
        }

        if (!peerPublicKeys.containsKey(idCredR)) {
            LOGGER.error("R_M2: The identity expressed by ID_CRED_R is not recognized");
            return false;
        }

        if (ownIdCreds.contains(idCredR)) {
            LOGGER.error("R_M2: The identity expressed by ID_CRED_R is equal to my own identity");
            return false;
        }

        // Invoke the retrieval and/or validation of CRED_R and
        // the processing of possible EAD items in EAD_2
        SideProcessor sideProcessor = session.getSideProcessor();
        CBORObject[] sideProcessorInfo = new CBORObject[3];

        sideProcessorInfo[0] = CBORObject.FromObject(gY);
        sideProcessorInfo[1] = CBORObject.FromObject(connectionIdentifierResponder);
        sideProcessorInfo[2] = CBORObject.FromObject(idCredR);

        sideProcessor.sideProcessingMessage2PreVerification(sideProcessorInfo, ead2);

        // An error occurred
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, false)
                .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

            String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, false)
                    .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                    .get(0)
                    .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                    .AsString();

            // No need to keep this information any longer in the side processor object
            sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_2, Constants.SIDE_PROCESSOR_OUTER_ERROR, false);

            LOGGER.error("R_M2: Using side processor pre verification on message 2: {}", error);
            return false;
        }

        // The side processor object includes the authentication credential
        // of the other peer if a valid one was found during the side processing
        CBORObject peerCredCBOR = null;
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, false)
                .containsKey(Constants.SIDE_PROCESSOR_OUTER_CRED)) {

            peerCredCBOR = sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, false)
                    .get(Constants.SIDE_PROCESSOR_OUTER_CRED)
                    .get(0)
                    .get(Constants.SIDE_PROCESSOR_INNER_CRED_VALUE);

            if (peerCredCBOR == null) {
                LOGGER.error("R_M2: Unable to retrieve the peer credential from the side processing on message 2");
                return false;
            }
        }

        // No need to keep this information any longer in the side processor object
        sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_2, Constants.SIDE_PROCESSOR_OUTER_CRED, false);

        // Compute PRK_3e2m
        OneKey peerLongTermKey = peerPublicKeys.get(idCredR);
        byte[] prk3e2m = computePRK3e2m(session, prk2e, th2, peerLongTermKey, peerEphemeralKey);
        if (prk3e2m == null) {
            LOGGER.error("R_M2: Computing PRK_3e2m");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_3e2m", prk3e2m));

        if (peerCredCBOR == null) {
            LOGGER.error("R_M2: Unable to retrieve the peer credential");
            return false;
        }
        byte[] peerCredential = peerCredCBOR.GetByteString();

        // Compute MAC_2
        byte[] mac2 = computeMAC2(session, prk3e2m, th2, cR, idCredR, peerCredential, ead2);
        if (mac2 == null) {
            LOGGER.error("R_M2: Computing MAC_2");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("MAC_2", mac2));

        // Verify Signature_or_MAC_2
        byte[] signatureOrMac2 = plaintextElementList[baseIndex + 1].GetByteString();
        LOGGER.debug(EdhocUtil.byteArrayToString("Signature_or_MAC_2", signatureOrMac2));

        // Prepare the External Data, as a CBOR sequence
        byte[] externalData = computeExternalData(th2, peerCredential, ead2);
        if (externalData == null) {
            LOGGER.error("R_M2: Computing External Data for MAC_2");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("External Data to verify Signature_or_MAC_2", externalData));

        if (!verifySignatureOrMac2(session, peerLongTermKey, idCredR, signatureOrMac2, externalData, mac2)) {
            LOGGER.error("R_M2: Non valid Signature_or_MAC_2");
            return false;
        }

        // Invoke the processing of possible EAD items in EAD_2
        // that had to wait for a successful verification of Signature_or_MAC_2
        if (ead2 != null && ead2.length > 0) {
            sideProcessor.sideProcessingMessage2PostVerification(sideProcessorInfo, ead2);

            // An error occurred
            if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, true)
                    .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

                String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, true)
                        .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                        .get(0)
                        .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                        .AsString();

                // No need to keep this information any longer in the side processor object
                sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_2, Constants.SIDE_PROCESSOR_OUTER_ERROR, true);

                LOGGER.error("R_M2: Using side processor post verification on message 2: {}", error);
                return false;
            }
        }

        /* Modify session */
        if (session.isInitiator()) {
            session.setPeerConnectionId(connectionIdentifierResponder);
            session.setPeerIdCred(idCredR);
            session.setPeerCred(peerCredential);
            session.setPeerEphemeralPublicKey(peerEphemeralKey);
            session.setPeerLongTermPublicKey(peerLongTermKey);
            session.setTH2(th2);
            session.setPlaintext2(plaintext2);
            session.setPRK2e(prk2e);
            session.setPRK3e2m(prk3e2m);
        }

        LOGGER.debug("Successful processing of EDHOC Message 2");
        return true;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage3}
     */
    public byte[] writeMessage3() {
        LOGGER.debug("Start of writeMessage3");
        EdhocSessionPersistent session = edhocMapperState.getEdhocSessionPersistent();
        List<CBORObject> objectList = new ArrayList<>();

        /* Start preparing data_3 */

        // C_R, if EDHOC message_3 is transported in a CoAP request, and it is enabled
        if (edhocMapperState.sendWithPrependedCX()) {
            byte[] connectionIdentifierResponder = session.getPeerConnectionId();
            CBORObject cR = encodeIdentifier(connectionIdentifierResponder);
            LOGGER.debug(EdhocUtil.byteArrayToString("Connection Identifier of the Responder",
                    connectionIdentifierResponder));
            LOGGER.debug(EdhocUtil.byteArrayToString("C_R", cR.EncodeToBytes()));
            objectList.add(cR);
        }

        /* Start computing the inner COSE object */

        // Compute TH_3
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());
        byte[] th2 = session.getTH2();
        byte[] th2SerializedCBOR = CBORObject.FromObject(th2).EncodeToBytes();
        byte[] plaintext2 = session.getPlaintext2();
        byte[] th3 = null;

        if (hasProtocolVersionLeqV15()) {
            th3 = computeTH3(hashAlgorithm, th2SerializedCBOR, plaintext2, new byte[0]);
        } else {
            th3 = computeTH3(hashAlgorithm, th2SerializedCBOR, plaintext2, session.getPeerCred());
        }

        if (th3 == null) {
            LOGGER.error("W_M3: Computing TH_3");
            return null;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("TH_3", th3));

        // Compute the key material
        byte[] prk4e3m = computePRK4e3m(session, session.getPRK3e2m(), th3, session.getPeerLongTermPublicKey(),
                session.getPeerEphemeralPublicKey());

        if (prk4e3m == null) {
            LOGGER.error("W_M3: Computing PRK_4e3m");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_4e3m", prk4e3m));

        // Produce possible EAD items following early instructions from the application
        SideProcessor sideProcessor = session.getSideProcessor();

        sideProcessor.produceIndependentEADs(Constants.EDHOC_MESSAGE_3);

        // An error occurred
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, false)
                .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

            String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, false)
                    .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                    .get(0)
                    .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                    .AsString();

            // No need to keep this information any longer in the side processor object
            sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_3, Constants.SIDE_PROCESSOR_OUTER_ERROR, false);

            LOGGER.error("W_M3: Using side processor pre verification on message 3: {}", error);
            return null;
        }

        CBORObject[] ead3 = null;
        List<CBORObject> ead3List = sideProcessor.getProducedEADs(Constants.EDHOC_MESSAGE_3);
        if (ead3List != null && !ead3List.isEmpty()) {
            ead3 = ead3List.toArray(new CBORObject[ead3List.size()]);
        }

        /* Start computing Signature_or_MAC_3 */

        // Compute MAC_3
        byte[] mac3 = computeMAC3(session, prk4e3m, th3, session.getIdCred(), session.getCred(), ead3);

        if (mac3 == null) {
            LOGGER.error("W_M3: Computing MAC_3");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("MAC_3", mac3));

        // Compute Signature_or_MAC_3

        // Compute the external data for the external_aad, as a CBOR sequence
        byte[] externalData = computeExternalData(th3, session.getCred(), ead3);
        if (externalData == null) {
            LOGGER.error("W_M3: Computing the external data for MAC_3");
            return null;
        }

        byte[] signatureOrMac3 = computeSignatureOrMac3(session, mac3, externalData);
        if (signatureOrMac3 == null) {
            LOGGER.error("W_M3: Computing Signature_or_MAC_3");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("Signature_or_MAC_3", signatureOrMac3));

        /* Start computing CIPHERTEXT_3 */

        // Compute K_3 and IV_3 to protect the outer COSE object

        byte[] k3 = computeKeyOrIV3("KEY", session, th3, session.getPRK3e2m());
        if (k3 == null) {
            LOGGER.error("W_M3: Computing K_3");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("K_3", k3));

        byte[] iv3 = computeKeyOrIV3("IV", session, th3, session.getPRK3e2m());
        if (iv3 == null) {
            LOGGER.error("W_M3: Computing IV_3");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("IV_3", iv3));

        // Prepare the External Data as including only TH3
        externalData = th3;

        // Prepare the plaintext
        List<CBORObject> plaintextElementList = new ArrayList<>();
        CBORObject plaintextElement;

        if (session.getIdCred().ContainsKey(HeaderKeys.KID.AsCBOR())) {
            // ID_CRED_I is 'kid', whose value is the only thing to include in the plaintext
            CBORObject kid = session.getIdCred().get(HeaderKeys.KID.AsCBOR());
            plaintextElement = encodeIdentifier(kid.GetByteString());
        } else {
            plaintextElement = session.getIdCred();
        }

        plaintextElementList.add(plaintextElement);
        plaintextElementList.add(CBORObject.FromObject(signatureOrMac3));
        if (ead3 != null && ead3.length > 0) {
            Collections.addAll(plaintextElementList, ead3);
        }

        byte[] plaintext3 = EdhocUtil.buildCBORSequence(plaintextElementList);
        LOGGER.debug(EdhocUtil.byteArrayToString("Plaintext to compute CIPHERTEXT_3", plaintext3));

        // Compute CIPHERTEXT_3 and add it to the outer CBOR sequence

        byte[] ciphertext3 = computeCiphertext3(session.getSelectedCipherSuite(), externalData, plaintext3, k3, iv3);
        LOGGER.debug(EdhocUtil.byteArrayToString("CIPHERTEXT_3", ciphertext3));
        objectList.add(CBORObject.FromObject(ciphertext3));

        /* Compute TH4 */

        byte[] th3SerializedCBOR = CBORObject.FromObject(th3).EncodeToBytes();
        byte[] th4 = null;

        if (hasProtocolVersionLeqV15()) {
            th4 = computeTH4(hashAlgorithm, th3SerializedCBOR, plaintext3, new byte[0]);
        } else {
            th4 = computeTH4(hashAlgorithm, th3SerializedCBOR, plaintext3, session.getCred());
        }

        if (th4 == null) {
            LOGGER.error("W_M3: Computing TH_4");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("TH_4", th4));

        /* Compute PRK_out */
        byte[] prkOut = computePRKout(session, th4, prk4e3m);
        if (prkOut == null) {
            LOGGER.error("W_M3: Computing PRK_out");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_out", prkOut));

        /* Compute PRK_exporter */
        byte[] prkExporter = computePRKexporter(session, prkOut);
        if (prkExporter == null) {
            LOGGER.error("W_M3: Computing PRK_exporter");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_exporter", prkExporter));

        /* Prepare EDHOC Message 3 */
        byte[] message3 = EdhocUtil.buildCBORSequence(objectList);
        LOGGER.debug(EdhocUtil.byteArrayToString("EDHOC Message 3", message3));

        /* Modify session and derive new oscore context */
        if (session.isInitiator()) {
            session.setTH3(th3);
            session.setPRK4e3m(prk4e3m);
            session.setTH4(th4);
            session.setPRKout(prkOut);
            session.setPRKexporter(prkExporter);
            session.setMessage3(message3);

            // derive new oscore context
            session.setupOscoreContext();
        }

        return message3;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage4}
     */
    public boolean readMessage4(byte[] sequence) {
        LOGGER.debug("Start of readMessage4");
        Map<CBORObject, EdhocSessionPersistent> edhocSessions = edhocMapperState.getEdhocEndpointInfoPersistent()
                .getEdhocSessionsPersistent();
        Set<CBORObject> usedConnectionIds = edhocMapperState.getEdhocEndpointInfoPersistent().getUsedConnectionIds();

        if (sequence == null || edhocSessions == null || usedConnectionIds == null) {
            LOGGER.error("R_M4: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.error("R_M4: Unable to decode byte sequence to CBOR object array");
            return false;
        }

        /* Consistency checks */

        // C_I
        byte[] connectionIdentifierInitiator;
        if (edhocMapperState.receiveWithPrependedCX()) {
            // CoAP Server as Initiator
            // Message 4 is transported in a CoAP request
            // C_I is present as first element of the CBOR sequence
            index++;
            if (objectListRequest[index].getType() != CBORType.ByteString
                    && objectListRequest[index].getType() != CBORType.Integer) {
                LOGGER.error("R_M4: C_I must be a byte string or an integer");
                return false;
            }

            connectionIdentifierInitiator = decodeIdentifier(objectListRequest[index]);
            if (connectionIdentifierInitiator == null) {
                LOGGER.error("R_M4: Invalid encoding of C_I");
                return false;
            }
        } else {
            // CoAP Client as Initiator when Message 4 is a CoAP response of a previous
            // Message 3 request
            // or CoAP Server as Initiator with correlation with CX is disabled
            connectionIdentifierInitiator = edhocMapperState.getEdhocSessionPersistent().getConnectionId();
        }

        CBORObject connectionIdentifierInitiatorCbor = CBORObject.FromObject(connectionIdentifierInitiator);
        EdhocSessionPersistent session = edhocSessions.get(connectionIdentifierInitiatorCbor);

        if (session == null) {
            LOGGER.error("R_M4: EDHOC session not found");
            return false;
        }

        // CIPHERTEXT_4
        index++;
        byte[] ciphertext4;
        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.error("R_M4: CIPHERTEXT_4 must be a byte string");
            return false;
        }

        ciphertext4 = objectListRequest[index].GetByteString();
        if (ciphertext4 == null) {
            LOGGER.error("R_M4: Retrieving CIPHERTEXT_4");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("CIPHERTEXT_4", ciphertext4));

        /* Compute the plaintext */

        // Compute the external data for the external_aad

        // Prepare the External Data as including only TH4
        byte[] externalData = session.getTH4();

        if (externalData == null) {
            LOGGER.error("R_M4: Computing the external data for CIPHERTEXT_4");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("External Data to compute CIPHERTEXT_4", externalData));

        // Compute the key material

        // Compute K and IV to protect the COSE object

        byte[] k4ae = computeKeyOrIV4("KEY", session, session.getTH4(), session.getPRK4e3m());
        if (k4ae == null) {
            LOGGER.error("R_M4: Computing K");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("K", k4ae));

        byte[] iv4ae = computeKeyOrIV4("IV", session, session.getTH4(), session.getPRK4e3m());
        if (iv4ae == null) {
            LOGGER.error("R_M4: Computing IV");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("IV", iv4ae));

        byte[] plaintext4 = decryptCiphertext4(session.getSelectedCipherSuite(), externalData, ciphertext4, k4ae,
                iv4ae);
        if (plaintext4 == null) {
            LOGGER.error("R_M4: Decrypting CIPHERTEXT_4");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("Plaintext retrieved from CIPHERTEXT_4", plaintext4));

        // Parse the outer plaintext as a CBOR sequence. To be valid, this is either the
        // empty plaintext
        // or just padding or padding followed by the External Authorization Data EAD_4
        // possibly
        CBORObject[] ead4 = null;

        if (plaintext4.length > 0) {
            int baseIndex = 0;
            CBORObject[] plaintextElementList;

            try {
                plaintextElementList = CBORObject.DecodeSequenceFromBytes(plaintext4);
            } catch (Exception e) {
                LOGGER.error("R_M4: Malformed or invalid EAD_4");
                return false;
            }

            if (hasProtocolVersionLeqV17()) {
                // Discard possible padding prepended to the plaintext
                while (baseIndex < plaintextElementList.length
                        && plaintextElementList[baseIndex].equals(CBORObject.True)) {
                    baseIndex++;
                }
            }

            int length = plaintextElementList.length - baseIndex;
            if (length > 0) {
                // EAD_4 is present
                if (hasProtocolVersionLeqV17()) {
                    ead4 = preParseEADleqV17(plaintextElementList, baseIndex, session.getSupportedEADs());
                } else {
                    ead4 = preParseEAD(plaintextElementList, baseIndex, 4, session.getSupportedEADs());
                }

                if (ead4 == null) {
                    LOGGER.error("R_M4: Processing EAD_4");
                    return false;
                }

                SideProcessor sideProcessor = session.getSideProcessor();

                // Invoke the processing of possible EAD items in EAD_4
                if (ead4.length > 0) {

                    sideProcessor.sideProcessingMessage4(ead4);

                    // An error occurred
                    if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_4, false)
                            .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

                        String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_4, false)
                                .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                                .get(0)
                                .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                                .AsString();

                        // No need to keep this information any longer in the side processor object
                        sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_4, Constants.SIDE_PROCESSOR_OUTER_ERROR,
                                false);

                        LOGGER.error("R_M4: Using side processor on message 4: {}", error);
                        return false;
                    }
                }
            }
        }

        LOGGER.debug("Successful processing of EDHOC Message 4");
        return true;
    }

    /* Responder message functions -- only session of Responder is modified */

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage1}
     */
    public boolean readMessage1(byte[] sequence) {
        LOGGER.debug("Start of readMessage1");
        List<Integer> supportedCipherSuites = edhocMapperState.getEdhocEndpointInfoPersistent()
                .getSupportedCipherSuites();
        AppProfile appProfile = edhocMapperState.getEdhocSessionPersistent().getApplicationProfile();

        if (sequence == null || supportedCipherSuites == null || appProfile == null) {
            LOGGER.error("R_M1: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.error("R_M1: Unable to decode byte sequence to CBOR object array");
            return false;
        }

        /* Consistency checks */

        if (objectListRequest.length == 0) {
            LOGGER.error("R_M1: CBOR object array is empty");
            return false;
        }

        // If the received message is a request and the first element before the actual
        // message_1 is the
        // CBOR simple value 'true', it can be skipped
        if (edhocMapperState.receiveWithPrependedCX()) {
            index++;
            if (!objectListRequest[index].equals(CBORObject.True)) {
                LOGGER.error("R_M1: The first element must be the CBOR simple value 'true'");
                return false;
            }
        }

        // METHOD
        index++;
        if (objectListRequest[index].getType() != CBORType.Integer) {
            LOGGER.error("R_M1: Method must be an integer");
            return false;
        }

        // Check that the indicated authentication method is supported
        int method = objectListRequest[index].AsInt32();
        if (!appProfile.isAuthMethodSupported(method)) {
            LOGGER.error("R_M1: Authentication method '{}' is not supported", method);
            return false;
        }

        // SUITES_I
        index++;
        int indexSuitesI = index;
        int selectedCipherSuite;
        List<Integer> cipherSuitesToOffer = null;

        if (objectListRequest[index].getType() == CBORType.Integer) {
            if (objectListRequest[index].AsInt32() < 0) {
                LOGGER.error("R_M1: SUITES_I as an integer must be positive");
                return false;
            }

            // SUITES_I is the selected cipher suite
            selectedCipherSuite = objectListRequest[index].AsInt32();

            // This peer does not support the selected cipher suite
            if (!supportedCipherSuites.contains(selectedCipherSuite)) {
                LOGGER.error("R_M1: The selected cipher suite is not supported");
                // SUITES_R will include all the cipher suites supported by the Responder
                cipherSuitesToOffer = supportedCipherSuites;
            }

        } else if (objectListRequest[index].getType() == CBORType.Array) {
            if (objectListRequest[index].size() < 2) {
                LOGGER.error("R_M1: SUITES_I as an array must have at least 2 elements");
                return false;
            }

            for (int i = 0; i < objectListRequest[index].size(); i++) {
                if (objectListRequest[index].get(i).getType() != CBORType.Integer
                        || objectListRequest[index].get(i).AsInt32() < 0) {
                    LOGGER.error("R_M1: SUITES_I as an array must have positive integers as elements");
                    return false;
                }
            }

            // The selected cipher suite is the last element of SUITES_I
            int size = objectListRequest[index].size();
            selectedCipherSuite = objectListRequest[index].get(size - 1).AsInt32();

            int firstSharedCipherSuite = -1;
            // Find the first commonly supported cipher suite, i.e. the cipher suite both
            // supported by the Responder and specified as early as possible in SUITES_I
            for (int i = 0; i < size; i++) {
                int suite = objectListRequest[index].get(i).AsInt32();
                if (supportedCipherSuites.contains(suite)) {
                    firstSharedCipherSuite = suite;
                    break;
                }
            }

            if (!supportedCipherSuites.contains(selectedCipherSuite)) {
                // The Responder does not support the selected cipher suite
                LOGGER.error("R_M1: The selected cipher suite is not supported");

                if (firstSharedCipherSuite == -1) {
                    // The Responder does not support any cipher suites in SUITES_I.
                    // SUITES_R will include all the cipher suites supported by the Responder
                    cipherSuitesToOffer = supportedCipherSuites;
                } else {
                    // SUITES_R will include only the cipher suite supported
                    // by both peers and most preferred by the Initiator.
                    cipherSuitesToOffer = new ArrayList<>(firstSharedCipherSuite);
                }
            } else if (firstSharedCipherSuite != selectedCipherSuite) {
                // The Responder supports the selected cipher suite, but it has to reply with an
                // EDHOC Error Message
                // if it supports a cipher suite more preferred by the Initiator than the
                // selected cipher suite

                LOGGER.error("R_M1: The selected cipher suite is not supported");

                // SUITES_R will include only the cipher suite supported
                // by both peers and most preferred by the Initiator.
                cipherSuitesToOffer = new ArrayList<>(firstSharedCipherSuite);
            }
        } else {
            // SUITES_I is not cbor_integer nor cbor_array
            LOGGER.error("R_M1: SUITES_I must be integer or array");
            return false;
        }

        // G_X
        index++;
        int indexGX = index;
        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.error("R_M1: G_X must be a byte string");
            return false;
        }
        byte[] gX = objectListRequest[index].GetByteString();

        // C_I
        index++;
        if (objectListRequest[index].getType() != CBORType.ByteString
                && objectListRequest[index].getType() != CBORType.Integer) {
            LOGGER.error("R_M1: C_I must be a byte string or an integer");
            return false;
        }

        // The Connection Identifier C_I as encoded in the EDHOC message
        CBORObject cI = objectListRequest[index];
        byte[] connectionIdInitiator = decodeIdentifier(cI);
        if (connectionIdInitiator == null) {
            LOGGER.error("R_M1: Invalid encoding of C_I");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("Connection Identifier of the Initiator", connectionIdInitiator));
        LOGGER.debug(EdhocUtil.byteArrayToString("C_I", cI.EncodeToBytes()));

        // EAD_1
        index++;
        CBORObject[] ead1 = null;
        int length = objectListRequest.length - index;
        if (length > 0) {
            // EAD_1 is present
            if (hasProtocolVersionLeqV17()) {
                ead1 = preParseEADleqV17(objectListRequest, index,
                        edhocMapperState.getEdhocSessionPersistent().getSupportedEADs());
            } else {
                ead1 = preParseEAD(objectListRequest, index, 1,
                        edhocMapperState.getEdhocSessionPersistent().getSupportedEADs());
            }

            if (ead1 == null) {
                LOGGER.error("R_M1: Processing EAD_1");
                return false;
            }

            // Process possible EAD items using the side processor
            if (ead1.length > 0) {
                CBORObject[] sideProcessorInfo = new CBORObject[4];

                // add Method
                sideProcessorInfo[0] = CBORObject.FromObject(method);

                // add SUITES_I
                if (objectListRequest[indexSuitesI].getType() == CBORType.Integer) {
                    // Single-element array is needed, with a single CBOR integer
                    // indicating the selected cipher suite
                    sideProcessorInfo[1] = CBORObject.NewArray();
                    sideProcessorInfo[1].Add(objectListRequest[indexSuitesI]);
                } else {
                    // SUITES_I can be taken as is, with the last integer
                    // indicating the selected cipher suite
                    sideProcessorInfo[1] = objectListRequest[indexSuitesI];
                }

                // add G_X
                sideProcessorInfo[2] = objectListRequest[indexGX];

                // add C_I
                sideProcessorInfo[3] = CBORObject.FromObject(connectionIdInitiator);

                SideProcessor sideProcessor = edhocMapperState.getEdhocSessionPersistent().getSideProcessor();

                sideProcessor.sideProcessingMessage1(sideProcessorInfo, ead1);

                // An error occurred
                if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_1, false)
                        .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

                    String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_1, false)
                            .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                            .get(0)
                            .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                            .AsString();

                    // No need to keep this information any longer in the side processor object
                    sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_1, Constants.SIDE_PROCESSOR_OUTER_ERROR,
                            false);

                    LOGGER.error("R_M1: Using side processor on message 1: {}", error);
                    return false;
                }
            }
        }

        /*
         * Modify session -- Create a new edhocSessionPersistent to replace current
         * session
         */

        EdhocSessionPersistent oldSession = edhocMapperState.getEdhocSessionPersistent();
        EdhocEndpointInfoPersistent endpointInfo = edhocMapperState.getEdhocEndpointInfoPersistent();

        if (oldSession.isInitiator()) {
            // In case an Initiator calls this function,
            // do not update its session
            return true;
        }

        byte[] connectionIdResponder = oldSession.getConnectionId();
        if (edhocMapperState.getEdhocMapperConfig().generateOwnConnectionId()) {
            connectionIdResponder = Util.getConnectionId(endpointInfo.getUsedConnectionIds(),
                    endpointInfo.getOscoreDb(), connectionIdInitiator);
        }

        EdhocSessionPersistent newSession = new EdhocSessionPersistent(oldSession.getSessionUri(),
                oldSession.isInitiator(), oldSession.isClientInitiated(), method, connectionIdResponder, endpointInfo,
                oldSession.getPeerSupportedCipherSuites(), endpointInfo.getOscoreDb(),
                oldSession.getCoapExchanger(), oldSession.isSessionResetEnabled(),
                oldSession.getForceOscoreSenderId(), oldSession.getForceOscoreRecipientId());

        // Set the selected cipher suite
        newSession.setSelectedCipherSuite(selectedCipherSuite);

        // Set the asymmetric key pair, CRED and ID_CRED of the Responder to use in this
        // session
        newSession.setAuthenticationCredential();

        // Set the Connection Identifier of the peer
        newSession.setPeerConnectionId(connectionIdInitiator);

        // Set the ephemeral public key of the Initiator
        OneKey peerEphemeralKey = switch (selectedCipherSuite) {
            case Constants.EDHOC_CIPHER_SUITE_0, Constants.EDHOC_CIPHER_SUITE_1 ->
                SharedSecretCalculation.buildCurve25519OneKey(null, gX);
            case Constants.EDHOC_CIPHER_SUITE_2, Constants.EDHOC_CIPHER_SUITE_3 ->
                SharedSecretCalculation.buildEcdsa256OneKey(null, gX, null);
            default ->
                throw new IllegalStateException("Unexpected value: " + selectedCipherSuite);
        };

        newSession.setPeerEphemeralPublicKey(peerEphemeralKey);

        // Compute and store the hash of EDHOC Message 1
        // If CX is prepended it must be skipped and not be in the hash
        int offset = edhocMapperState.receiveWithPrependedCX() ? 1 : 0;
        byte[] hashMessage1 = new byte[sequence.length - offset];
        System.arraycopy(sequence, offset, hashMessage1, 0, hashMessage1.length);
        newSession.setHashMessage1(hashMessage1);

        // Set cipherSuites to offer in next Error Message
        if (cipherSuitesToOffer != null) {
            newSession.setCipherSuitesIncludeInError(Util.buildSuitesR(cipherSuitesToOffer));
        }

        // Replace old session
        edhocMapperState.setEdhocSessionPersistent(newSession);

        // Update edhocSessions
        edhocMapperState.getEdhocEndpointInfoPersistent().getEdhocSessionsPersistent()
                .put(CBORObject.FromObject(connectionIdResponder), newSession);

        LOGGER.debug("Successful processing of EDHOC Message 1");
        return true;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage2}
     */
    public byte[] writeMessage2() {
        LOGGER.debug("Start of writeMessage2");
        EdhocSessionPersistent session = edhocMapperState.getEdhocSessionPersistent();
        List<CBORObject> objectList = new ArrayList<>();

        // C_I, if EDHOC message_2 is transported in a CoAP request and CX correlation
        // is enabled
        if (edhocMapperState.sendWithPrependedCX()) {
            byte[] connectionIdentifierInitiator = session.getPeerConnectionId();
            CBORObject cI = encodeIdentifier(connectionIdentifierInitiator);
            LOGGER.debug(EdhocUtil.byteArrayToString("Connection Identifier of the Initiator",
                    connectionIdentifierInitiator));
            LOGGER.debug(EdhocUtil.byteArrayToString("C_I", cI.EncodeToBytes()));
            objectList.add(cI);
        }

        // Set the ephemeral keys to use in this session
        if (session.getEphemeralKey() == null) {
            session.setEphemeralKey();
        }

        // G_Y as a CBOR byte string
        int selectedSuite = session.getSelectedCipherSuite();
        CBORObject gY = switch (selectedSuite) {
            case Constants.EDHOC_CIPHER_SUITE_0, Constants.EDHOC_CIPHER_SUITE_1 ->
                session.getEphemeralKey().PublicKey().get(KeyKeys.OKP_X);
            case Constants.EDHOC_CIPHER_SUITE_2, Constants.EDHOC_CIPHER_SUITE_3 ->
                session.getEphemeralKey().PublicKey().get(KeyKeys.EC2_X);
            default ->
                null;
        };

        if (gY == null) {
            LOGGER.error("W_M2: Invalid G_Y");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("G_Y", gY.GetByteString()));

        // C_R
        byte[] connectionIdentifierResponder = session.getConnectionId();
        CBORObject cR = encodeIdentifier(connectionIdentifierResponder);
        LOGGER.debug(
                EdhocUtil.byteArrayToString("Connection Identifier of the Responder", connectionIdentifierResponder));
        LOGGER.debug(EdhocUtil.byteArrayToString("C_R", cR.EncodeToBytes()));

        // Compute TH_2
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(selectedSuite);
        byte[] hashMessage1 = session.getHashMessage1();
        byte[] hashMessage1SerializedCBOR = CBORObject.FromObject(hashMessage1).EncodeToBytes();
        byte[] gYSerializedCBOR = gY.EncodeToBytes();

        byte[] th2 = null;
        if (hasProtocolVersionLeqV19()) {
            th2 = computeTH2(hashAlgorithm, gYSerializedCBOR, cR.EncodeToBytes(), hashMessage1SerializedCBOR);
        } else {
            th2 = computeTH2(hashAlgorithm, gYSerializedCBOR, new byte[0], hashMessage1SerializedCBOR);
        }

        if (th2 == null) {
            LOGGER.error("W_M2: Computing TH_2");
            return null;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("H(message_1)", hashMessage1));
        LOGGER.debug(EdhocUtil.byteArrayToString("TH_2", th2));

        // Compute the key material

        // Compute the Diffie-Hellman secret G_XY
        byte[] dhSecret = SharedSecretCalculation.generateSharedSecret(session.getEphemeralKey(),
                session.getPeerEphemeralPublicKey());

        if (dhSecret == null) {
            LOGGER.error("W_M2: Computing the Diffie-Hellman Secret");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("G_XY", dhSecret));

        // Compute PRK_2e
        byte[] prk2e = null;
        if (hasProtocolVersionLeqV15()) {
            prk2e = computePRK2e(new byte[0], dhSecret, hashAlgorithm);
        } else {
            prk2e = computePRK2e(th2, dhSecret, hashAlgorithm);
        }

        if (prk2e == null) {
            LOGGER.error("W_M2: Computing PRK_2e");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_2e", prk2e));

        // Compute PRK_3e2m
        byte[] prk3e2m = computePRK3e2m(session, prk2e, th2, session.getPeerLongTermPublicKey(),
                session.getPeerEphemeralPublicKey());

        if (prk3e2m == null) {
            LOGGER.error("W_M2: Computing PRK_3e2m");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_3e2m", prk3e2m));

        // Produce possible EAD items following early instructions from the application
        SideProcessor sideProcessor = session.getSideProcessor();

        sideProcessor.produceIndependentEADs(Constants.EDHOC_MESSAGE_2);

        // An error occurred
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, false)
                .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

            String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_2, false)
                    .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                    .get(0)
                    .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                    .AsString();

            // No need to keep this information any longer in the side processor object
            sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_2, Constants.SIDE_PROCESSOR_OUTER_ERROR, false);

            LOGGER.error("W_M2: Using side processor pre verification on message 2: {}", error);
            return null;
        }

        CBORObject[] ead2 = null;
        List<CBORObject> ead2List = sideProcessor.getProducedEADs(Constants.EDHOC_MESSAGE_2);
        if (ead2List != null && !ead2List.isEmpty()) {
            ead2 = ead2List.toArray(new CBORObject[ead2List.size()]);
        }

        /* Start computing Signature_or_MAC_2 */

        // Compute MAC_2
        byte[] mac2 = computeMAC2(session, prk3e2m, th2, cR, session.getIdCred(), session.getCred(), ead2);
        if (mac2 == null) {
            LOGGER.error("W_M2: Computing MAC_2");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("MAC_2", mac2));

        // Compute Signature_or_MAC_2

        // Compute the external data for the external_aad, as a CBOR sequence
        byte[] externalData = computeExternalData(th2, session.getCred(), ead2);
        if (externalData == null) {
            LOGGER.error("W_M2: Computing the external data for MAC_2");
            return null;
        }

        byte[] signatureOrMac2 = computeSignatureOrMac2(session, mac2, externalData);

        if (signatureOrMac2 == null) {
            LOGGER.error("W_M2: Computing Signature_or_MAC_2");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("Signature_or_MAC_2", signatureOrMac2));

        /* Start computing CIPHERTEXT_2 */

        // Prepare the plaintext
        List<CBORObject> plaintextElementList = new ArrayList<>();
        CBORObject plaintextElement;

        if (!hasProtocolVersionLeqV19()) {
            plaintextElementList.add(cR);
        }

        if (session.getIdCred().ContainsKey(HeaderKeys.KID.AsCBOR())) {
            // ID_CRED_R uses 'kid', whose value is the only thing to include in the
            // plaintext
            CBORObject kid = session.getIdCred().get(HeaderKeys.KID.AsCBOR());
            plaintextElement = encodeIdentifier(kid.GetByteString());
        } else {
            plaintextElement = session.getIdCred();
        }

        plaintextElementList.add(plaintextElement);
        plaintextElementList.add(CBORObject.FromObject(signatureOrMac2));

        if (ead2 != null && ead2.length > 0) {
            Collections.addAll(plaintextElementList, ead2);
        }

        byte[] plaintext2 = EdhocUtil.buildCBORSequence(plaintextElementList);
        LOGGER.debug(EdhocUtil.byteArrayToString("Plaintext to compute CIPHERTEXT_2", plaintext2));

        // Compute KEYSTREAM_2
        byte[] keystream2 = computeKeystream2(session, th2, prk2e, plaintext2.length);
        if (keystream2 == null) {
            LOGGER.error("W_M2: Computing KEYSTREAM_2");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("KEYSTREAM_2", keystream2));

        // Compute CIPHERTEXT_2
        byte[] ciphertext2 = EdhocUtil.arrayXor(plaintext2, keystream2);

        LOGGER.debug(EdhocUtil.byteArrayToString("CIPHERTEXT_2", ciphertext2));

        // Concatenate G_Y with CIPHERTEXT_2
        byte[] gY_Ciphertext2 = new byte[gY.GetByteString().length + ciphertext2.length];
        System.arraycopy(gY.GetByteString(), 0, gY_Ciphertext2, 0, gY.GetByteString().length);
        System.arraycopy(ciphertext2, 0, gY_Ciphertext2, gY.GetByteString().length, ciphertext2.length);

        // Wrap the result in a single CBOR byte string, included in the outer CBOR
        // sequence of EDHOC Message 2
        objectList.add(CBORObject.FromObject(gY_Ciphertext2));
        LOGGER.debug(EdhocUtil.byteArrayToString("G_Y | CIPHERTEXT_2", gY_Ciphertext2));

        if (hasProtocolVersionLeqV19()) {
            // The outer CBOR sequence finishes with the connection identifier C_R
            objectList.add(cR);
        }

        /* Prepare EDHOC Message 2 */
        byte[] message2 = EdhocUtil.buildCBORSequence(objectList);
        LOGGER.debug(EdhocUtil.byteArrayToString("EDHOC Message 2", message2));

        /* Modify session */
        if (!session.isInitiator()) {
            session.setTH2(th2);
            session.setPRK2e(prk2e);
            session.setPRK3e2m(prk3e2m);
            session.setPlaintext2(plaintext2);
        }

        return message2;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#readMessage3}
     */
    public boolean readMessage3(byte[] sequence) {
        LOGGER.debug("Start of readMessage3");
        Map<CBORObject, EdhocSessionPersistent> edhocSessions = edhocMapperState.getEdhocEndpointInfoPersistent()
                .getEdhocSessionsPersistent();
        Map<CBORObject, OneKey> peerPublicKeys = edhocMapperState.getEdhocEndpointInfoPersistent().getPeerPublicKeys();
        Map<CBORObject, CBORObject> peerCredentials = edhocMapperState.getEdhocEndpointInfoPersistent()
                .getPeerCredentials();
        Set<CBORObject> usedConnectionIds = edhocMapperState.getEdhocEndpointInfoPersistent().getUsedConnectionIds();

        if (sequence == null || edhocSessions == null || peerPublicKeys == null || peerCredentials == null
                || usedConnectionIds == null) {
            LOGGER.error("R_M3: Null initial parameters");
            return false;
        }

        int index = -1;
        CBORObject[] objectListRequest;
        try {
            objectListRequest = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.error("R_M3: Unable to decode byte sequence to CBOR object array");
            return false;
        }

        /* Consistency checks */

        // C_R
        byte[] connectionIdentifierResponder;
        if (edhocMapperState.receiveWithPrependedCX()) {
            // CoAP Server as Responder
            // Message 3 is transported in a CoAP request,
            // C_R is present as first element of the CBOR sequence
            index++;
            if (objectListRequest[index].getType() != CBORType.ByteString
                    && objectListRequest[index].getType() != CBORType.Integer) {
                LOGGER.error("R_M3: C_R must be a byte string or an integer");
                return false;
            }

            connectionIdentifierResponder = decodeIdentifier(objectListRequest[index]);
            if (connectionIdentifierResponder == null) {
                LOGGER.error("R_M3: Invalid encoding of C_R");
                return false;
            }
        } else {
            // CoAP Client as Responder when Message 3 is a CoAP response of a previous
            // Message 2 request
            // or CoAP Server as Initiator with CX correlation disabled
            connectionIdentifierResponder = edhocMapperState.getEdhocSessionPersistent().getConnectionId();
        }

        CBORObject connectionIdentifierResponderCbor = CBORObject.FromObject(connectionIdentifierResponder);
        EdhocSessionPersistent session = edhocSessions.get(connectionIdentifierResponderCbor);

        if (session == null) {
            LOGGER.error("R_M3: EDHOC session not found");
            return false;
        }

        // CIPHERTEXT_3
        index++;
        if (objectListRequest[index].getType() != CBORType.ByteString) {
            LOGGER.error("R_M3: CIPHERTEXT_3 must be a byte string");
            return false;
        }
        byte[] ciphertext3 = objectListRequest[index].GetByteString();
        LOGGER.debug(EdhocUtil.byteArrayToString("CIPHERTEXT_3", ciphertext3));

        /* Decrypt CIPHERTEXT_3 */

        // Compute TH3
        String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());
        byte[] th2 = session.getTH2();
        byte[] th2SerializedCBOR = CBORObject.FromObject(th2).EncodeToBytes();
        byte[] plaintext2 = session.getPlaintext2();
        byte[] th3 = null;

        if (hasProtocolVersionLeqV15()) {
            th3 = computeTH3(hashAlgorithm, th2SerializedCBOR, plaintext2, new byte[0]);
        } else {
            th3 = computeTH3(hashAlgorithm, th2SerializedCBOR, plaintext2, session.getCred());
        }

        if (th3 == null) {
            LOGGER.error("R_M3: Computing TH3");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("TH_3", th3));

        // Compute K_3 and IV_3 to protect the outer COSE object
        byte[] k3 = computeKeyOrIV3("KEY", session, th3, session.getPRK3e2m());
        if (k3 == null) {
            LOGGER.error("R_M3: Computing TH3");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("K_3", k3));

        byte[] iv3 = computeKeyOrIV3("IV", session, th3, session.getPRK3e2m());
        if (iv3 == null) {
            LOGGER.error("R_M3: Computing IV_3ae");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("IV_3", iv3));

        // Prepare the external_aad as including only TH3
        byte[] externalData = th3;

        // Compute the plaintext
        byte[] plaintext3 = decryptCiphertext3(session.getSelectedCipherSuite(), externalData, ciphertext3, k3, iv3);
        if (plaintext3 == null) {
            LOGGER.error("R_M3: Decrypting CIPHERTEXT_3");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("Plaintext retrieved from CIPHERTEXT_3", plaintext3));

        // Parse the outer plaintext as a CBOR sequence
        int baseIndex = 0;
        CBORObject[] plaintextElementList;
        try {
            plaintextElementList = CBORObject.DecodeSequenceFromBytes(plaintext3);
        } catch (Exception e) {
            LOGGER.error("R_M3: Malformed or invalid plaintext from CIPHERTEXT_3");
            return false;
        }

        if (plaintextElementList.length == 0) {
            LOGGER.error("R_M3: Zero-length plaintext_3");
            return false;
        }

        if (hasProtocolVersionLeqV17()) {
            // Discard possible padding prepended to the plaintext
            while (baseIndex < plaintextElementList.length
                    && plaintextElementList[baseIndex].equals(CBORObject.True)) {
                baseIndex++;
            }
        }

        // ID_CRED_I and Signature_or_MAC_3 should be contained
        if (plaintextElementList.length - baseIndex < 2) {
            LOGGER.error("R_M3: Plaintext_3 contains less than two elements");
            return false;
        }

        // check ID_CRED_I
        if (plaintextElementList[baseIndex].getType() != CBORType.ByteString
                && plaintextElementList[baseIndex].getType() != CBORType.Integer
                && plaintextElementList[baseIndex].getType() != CBORType.Map) {
            LOGGER.error("R_M3: Invalid type of ID_CRED_I in plaintext_3");
            return false;
        }

        // check Signature_or_MAC_3
        if (plaintextElementList[baseIndex + 1].getType() != CBORType.ByteString) {
            LOGGER.error("R_M3: Signature_or_MAC_3 must be a byte string");
            return false;
        }

        // check EAD_3
        CBORObject[] ead3 = null;
        int length = plaintextElementList.length - baseIndex - 2;
        if (length > 0) {
            // EAD_3 is present
            if (hasProtocolVersionLeqV17()) {
                ead3 = preParseEADleqV17(plaintextElementList, baseIndex + 2, session.getSupportedEADs());
            } else {
                ead3 = preParseEAD(plaintextElementList, baseIndex + 2, 3, session.getSupportedEADs());
            }

            if (ead3 == null) {
                LOGGER.error("R_M3: Processing EAD_3");
                return false;
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
            LOGGER.error("R_M3: Invalid format for ID_CRED_I");
            return false;
        }

        if (!peerPublicKeys.containsKey(idCredI)) {
            LOGGER.error("R_M3: The identity expressed by ID_CRED_I is not recognized");
            return false;
        }

        // Invoke the retrieval and/or validation of CRED_I
        // and the processing of possible EAD items in EAD_3
        SideProcessor sideProcessor = session.getSideProcessor();
        CBORObject[] sideProcessorInfo = new CBORObject[1];
        sideProcessorInfo[0] = CBORObject.FromObject(idCredI);

        sideProcessor.sideProcessingMessage3PreVerification(sideProcessorInfo, ead3);

        // An error occurred
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, false)
                .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

            String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, false)
                    .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                    .get(0)
                    .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                    .AsString();

            // No need to keep this information any longer in the side processor object
            sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_3, Constants.SIDE_PROCESSOR_OUTER_ERROR, false);

            LOGGER.error("R_M3: Using side processor pre verification on message 3: {}", error);
            return false;
        }

        // If no error occurred, the side processor object includes the authentication
        // credential
        // of the other peer, if a valid one was found during the side processing
        CBORObject peerCredCBOR = null;
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, false)
                .containsKey(Integer.valueOf(Constants.SIDE_PROCESSOR_OUTER_CRED))) {

            peerCredCBOR = sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, false)
                    .get(Integer.valueOf(Constants.SIDE_PROCESSOR_OUTER_CRED))
                    .get(0)
                    .get(Integer.valueOf(Constants.SIDE_PROCESSOR_INNER_CRED_VALUE));

            if (peerCredCBOR == null) {
                LOGGER.error("R_M3: Unable to retrieve the peer credential from the side processing on message 3");
                return false;
            }
        }

        // No need to keep this information any longer in the side processor object
        sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_3, Constants.SIDE_PROCESSOR_OUTER_CRED, false);

        // Compute the key material
        OneKey peerLongTermKey = peerPublicKeys.get(idCredI);
        byte[] prk4e3m = computePRK4e3m(session, session.getPRK3e2m(), th3, peerLongTermKey,
                session.getPeerEphemeralPublicKey());
        if (prk4e3m == null) {
            LOGGER.error("R_M3: Computing PRK_4e3m");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_4e3m", prk4e3m));

        /* Start verifying Signature_or_MAC_3 */

        if (peerCredCBOR == null) {
            LOGGER.error("R_M3: Unable to retrieve the peer credential");
            return false;
        }

        byte[] peerCredential = peerCredCBOR.GetByteString();

        // Compute MAC_3
        byte[] mac3 = computeMAC3(session, prk4e3m, th3, idCredI, peerCredential, ead3);
        if (mac3 == null) {
            LOGGER.error("R_M3: Computing MAC_3");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("MAC_3", mac3));

        // Verify Signature_or_MAC_3

        byte[] signatureOrMac3 = plaintextElementList[1].GetByteString();
        LOGGER.debug(EdhocUtil.byteArrayToString("Signature_or_MAC_3", signatureOrMac3));

        // Compute the external data, as a CBOR sequence
        externalData = computeExternalData(th3, peerCredential, ead3);
        if (externalData == null) {
            LOGGER.error("R_M3: Computing the external data for MAC_3");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("External Data to verify Signature_or_MAC_3", externalData));

        if (!verifySignatureOrMac3(session, peerLongTermKey, idCredI, signatureOrMac3, externalData, mac3)) {
            LOGGER.error("R_M3: Non valid Signature_or_MAC_3");
            return false;
        }

        // Invoke the processing of possible EAD items in EAD_3
        // that had to wait for a successful verification of Signature_or_MAC_3
        if (ead3 != null && ead3.length > 0) {
            sideProcessor.sideProcessingMessage3PostVerification(sideProcessorInfo, ead3);

            // An error occurred
            if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, true)
                    .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

                String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_3, true)
                        .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                        .get(0)
                        .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                        .AsString();

                // No need to keep this information any longer in the side processor object
                sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_3, Constants.SIDE_PROCESSOR_OUTER_ERROR, true);

                LOGGER.error("R_M3: Using side processor post verification on message 3: {}", error);
                return false;
            }
        }

        /* Compute TH4 */

        byte[] th3SerializedCBOR = CBORObject.FromObject(th3).EncodeToBytes();
        byte[] th4 = null;

        if (hasProtocolVersionLeqV15()) {
            th4 = computeTH4(hashAlgorithm, th3SerializedCBOR, plaintext3, new byte[0]);
        } else {
            th4 = computeTH4(hashAlgorithm, th3SerializedCBOR, plaintext3, peerCredential);
        }

        if (th4 == null) {
            LOGGER.error("R_M3: Computing TH_4");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("TH_4", th4));

        /* Compute PRK_out */
        byte[] prkOut = computePRKout(session, th4, prk4e3m);
        if (prkOut == null) {
            LOGGER.error("R_M3: Computing PRK_out");
            return false;
        }

        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_out", prkOut));

        /* Compute PRK_exporter */
        byte[] prkExporter = computePRKexporter(session, prkOut);
        if (prkExporter == null) {
            LOGGER.error("R_M3: Computing PRK_exporter");
            return false;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("PRK_exporter", prkExporter));

        /* Modify session and derive oscore context */
        if (!session.isInitiator()) {
            session.setTH3(th3);
            session.setPeerCred(peerCredential);
            session.setPeerIdCred(idCredI);
            session.setPeerLongTermPublicKey(peerLongTermKey);
            session.setPRK4e3m(prk4e3m);
            session.setTH4(th4);
            session.setPRKout(prkOut);
            session.setPRKexporter(prkExporter);

            // derive new oscore context
            session.setupOscoreContext();
        }

        LOGGER.debug("Successful processing of EDHOC Message 3");
        return true;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#writeMessage4}
     */
    public byte[] writeMessage4() {
        LOGGER.debug("Start of writeMessage4");
        EdhocSessionPersistent session = edhocMapperState.getEdhocSessionPersistent();
        List<CBORObject> objectList = new ArrayList<>();

        /* Start preparing data_4 */

        // C_I, if EDHOC message_4 is transported in a CoAP request and CX correlation
        // is enabled
        if (edhocMapperState.sendWithPrependedCX()) {
            byte[] connectionIdentifierInitiator = session.getPeerConnectionId();
            CBORObject cI = encodeIdentifier(connectionIdentifierInitiator);
            objectList.add(cI);
            LOGGER.debug(EdhocUtil.byteArrayToString("Connection Identifier of the Initiator",
                    connectionIdentifierInitiator));
            LOGGER.debug(EdhocUtil.byteArrayToString("C_I", cI.EncodeToBytes()));
        }

        /* Start computing the COSE object */

        // Compute the external data for the external_aad

        // Prepare the External Data as including only TH4
        byte[] externalData = session.getTH4();

        if (externalData == null) {
            LOGGER.error("W_M4: Computing the external data for CIPHERTEXT_4");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("External Data to compute CIPHERTEXT_4", externalData));

        // Produce possible EAD items following early instructions from the application
        SideProcessor sideProcessor = session.getSideProcessor();

        sideProcessor.produceIndependentEADs(Constants.EDHOC_MESSAGE_4);

        // An error occurred
        if (sideProcessor.getResults(Constants.EDHOC_MESSAGE_4, false)
                .containsKey(Constants.SIDE_PROCESSOR_OUTER_ERROR)) {

            String error = sideProcessor.getResults(Constants.EDHOC_MESSAGE_4, false)
                    .get(Constants.SIDE_PROCESSOR_OUTER_ERROR)
                    .get(0)
                    .get(Constants.SIDE_PROCESSOR_INNER_ERROR_DESCRIPTION)
                    .AsString();

            // No need to keep this information any longer in the side processor object
            sideProcessor.removeResultSet(Constants.EDHOC_MESSAGE_4, Constants.SIDE_PROCESSOR_OUTER_ERROR, false);

            LOGGER.error("W_M4: Using side processor on message 4: {}", error);
            return null;
        }

        CBORObject[] ead4 = null;
        List<CBORObject> ead4List = sideProcessor.getProducedEADs(Constants.EDHOC_MESSAGE_4);
        if (ead4List != null && !ead4List.isEmpty()) {
            ead4 = ead4List.toArray(new CBORObject[ead4List.size()]);
        }

        // Prepare the plaintext
        byte[] plaintext4 = new byte[] {};
        if (ead4 != null && ead4.length > 0) {
            List<CBORObject> plaintextElementList = new ArrayList<>();
            Collections.addAll(plaintextElementList, ead4);
            plaintext4 = EdhocUtil.buildCBORSequence(plaintextElementList);
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("Plaintext to compute CIPHERTEXT_4", plaintext4));

        // Compute the key material

        // Compute K and IV to protect the COSE object
        byte[] k4 = computeKeyOrIV4("KEY", session, session.getTH4(), session.getPRK4e3m());
        if (k4 == null) {
            LOGGER.error("W_M4: Computing K_4");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("K_4", k4));

        byte[] iv4 = computeKeyOrIV4("IV", session, session.getTH4(), session.getPRK4e3m());
        if (iv4 == null) {
            LOGGER.error("W_M4: Computing IV_4");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("IV_4", iv4));

        // Encrypt the COSE object and take the ciphertext as CIPHERTEXT_4
        byte[] ciphertext4 = computeCiphertext4(session.getSelectedCipherSuite(), externalData, plaintext4, k4, iv4);
        if (ciphertext4 == null) {
            LOGGER.error("W_M4: Computing CIPHERTEXT_4");
            return null;
        }
        LOGGER.debug(EdhocUtil.byteArrayToString("CIPHERTEXT_4", ciphertext4));
        objectList.add(CBORObject.FromObject(ciphertext4));

        /* Prepare EDHOC Message 4 */
        byte[] message4 = EdhocUtil.buildCBORSequence(objectList);
        LOGGER.debug(EdhocUtil.byteArrayToString("EDHOC Message 4", EdhocUtil.buildCBORSequence(objectList)));

        return message4;
    }

    /* Error message functions */

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#isErrorMessage}
     */
    protected boolean hasErrorMessageStructure(CBORObject[] myObjects) {
        // A CoAP message including an EDHOC error message is a CBOR sequence of
        // CX - not true (optional); ERR_CODE - int (mandatory); ERR_INFO - any type
        // (mandatory)
        if (myObjects.length != 3 && myObjects.length != 2) {
            return false;
        }

        if (edhocMapperState.receiveWithPrependedCX()) {
            // Received by CoAP server
            // Error message is a request, this starts with C_X different from 'true'
            // (0xf5),
            // followed by ERR_CODE as a CBOR integer
            return !myObjects[0].equals(CBORObject.True) && myObjects[1].getType() == CBORType.Integer;
        } else {
            // Received by CoAP client or CoAP server with CX correlation disabled
            // Error message is a response, this starts with ERR_CODE as a CBOR integer
            return myObjects[0].getType() == CBORType.Integer;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#writeErrorMessage}
     */
    public byte[] writeErrorMessage(int errorCode, String errMsg) {
        LOGGER.debug("Start of writeErrorMessage");

        CBORObject cipherSuites = edhocMapperState.getEdhocSessionPersistent().getCipherSuitesIncludeInError();

        if (cipherSuites != null && cipherSuites.getType() != CBORType.Integer
                && cipherSuites.getType() != CBORType.Array) {
            return null;
        }

        if (cipherSuites != null && cipherSuites.getType() == CBORType.Array) {
            for (int i = 0; i < cipherSuites.size(); i++) {
                if (cipherSuites.get(i).getType() != CBORType.Integer) {
                    return null;
                }
            }
        }

        List<CBORObject> objectList = new ArrayList<>();

        // Include C_X if error message sent from CoAP Client with CX correlation
        // enabled
        if (edhocMapperState.sendWithPrependedCX()) {
            CBORObject cX = encodeIdentifier(edhocMapperState.getEdhocSessionPersistent().getPeerConnectionId());
            objectList.add(cX);
        }

        // Include ERR_CODE
        objectList.add(CBORObject.FromObject(errorCode));

        // Include ERR_INFO
        if (errorCode == Constants.ERR_CODE_UNSPECIFIED_ERROR) {
            if (errMsg == null) {
                return null;
            }
            // Include DIAG_MSG
            objectList.add(CBORObject.FromObject(errMsg));
        } else if (errorCode == Constants.ERR_CODE_WRONG_SELECTED_CIPHER_SUITE) {
            // Possibly include cipher suites, this implies that EDHOC Message 1 was good
            // enough
            // to yield a suite negotiation
            if (cipherSuites != null)
                objectList.add(cipherSuites);
        }

        // Encode the EDHOC Error Message, as a CBOR sequence
        byte[] payload = EdhocUtil.buildCBORSequence(objectList);

        LOGGER.debug("Successful preparation of EDHOC Error Message");
        return payload;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#readErrorMessage}
     */
    public boolean readErrorMessage(byte[] sequence) {
        LOGGER.debug("Start of readErrorMessage");
        Map<CBORObject, EdhocSessionPersistent> edhocSessions = edhocMapperState.getEdhocEndpointInfoPersistent()
                .getEdhocSessionsPersistent();

        if (sequence == null || edhocSessions == null) {
            LOGGER.error("R_ERR: Null initial parameters");
            return false;
        }

        int index = 0;
        EdhocSessionPersistent session = null;
        CBORObject[] objectList;
        try {
            objectList = CBORObject.DecodeSequenceFromBytes(sequence);
        } catch (Exception e) {
            LOGGER.error("R_ERR: Malformed or invalid EDHOC Error Message");
            return false;
        }

        if (objectList.length == 0 || objectList.length > 3) {
            LOGGER.error("R_ERR: Zero or too many elements");
            return false;
        }

        if (edhocMapperState.receiveWithPrependedCX()) {
            // The connection identifier is expected as first element in the EDHOC Error
            // Message
            if (objectList[index].getType() != CBORType.ByteString
                    && objectList[index].getType() != CBORType.Integer) {
                LOGGER.error("R_ERR: Invalid format of C_X");
                return false;
            }

            byte[] retrievedConnectionIdentifier = decodeIdentifier(objectList[index]);
            if (retrievedConnectionIdentifier != null) {
                CBORObject connectionIdentifierCbor = CBORObject.FromObject(retrievedConnectionIdentifier);
                session = edhocSessions.get(connectionIdentifierCbor);
                index++;
            }
        } else {
            session = edhocMapperState.getEdhocSessionPersistent();
        }

        // No session for this Connection Identifier
        if (session == null) {
            LOGGER.error("R_ERR: Impossible to retrieve a session from C_X");
            return false;
        }

        if (objectList[index].getType() != CBORType.Integer) {
            LOGGER.error("R_ERR: Invalid format of ERR_CODE");
            return false;
        }

        // Retrieve ERR_CODE
        int errorCode = objectList[index].AsInt32();
        index++;

        // Check that the rest of the message is consistent
        if (objectList.length == index) {
            LOGGER.error("R_ERR: ERR_INFO expected but not included");
            return false;
        }

        if (objectList.length > (index + 1)) {
            LOGGER.error("R_ERR: Unexpected parameters following ERR_INFO");
            return false;
        }

        switch (errorCode) {
            case Constants.ERR_CODE_SUCCESS -> {
                LOGGER.warn("R_ERR: Error code success");
            }

            case Constants.ERR_CODE_UNSPECIFIED_ERROR -> {
                if (objectList[index].getType() != CBORType.TextString) {
                    LOGGER.error("R_ERR: Invalid format of ERR_INFO");
                    return false;
                }
                String errorMsg = objectList[index].AsString();
                LOGGER.info("ERR_INFO: {} ~ {}", EdhocUtil.byteArrayToString(errorMsg.getBytes(StandardCharsets.UTF_8)),
                        errorMsg);
            }

            case Constants.ERR_CODE_WRONG_SELECTED_CIPHER_SUITE -> {
                CBORObject suitesR = objectList[index];
                List<Integer> peerSupportedCipherSuites = new ArrayList<>();

                switch (suitesR.getType()) {
                    case Integer -> {
                        peerSupportedCipherSuites.add(suitesR.AsInt32());
                    }

                    case Array -> {
                        for (int i = 0; i < suitesR.size(); i++) {
                            if (suitesR.get(i).getType() != CBORType.Integer) {
                                LOGGER.error("R_ERR: Invalid format for elements of SUITES_R");
                                return false;
                            }
                            peerSupportedCipherSuites.add(suitesR.get(i).AsInt32());
                        }
                    }

                    default -> {
                        LOGGER.error("R_ERR: Invalid format for SUITES_R");
                        return false;
                    }
                }

                // add peer supported cipher suites to session
                session.setPeerSupportedCipherSuites(peerSupportedCipherSuites);
            }

            default -> {
                LOGGER.warn("R_ERR: Unknown error code: " + errorCode);
            }
        }

        return true;
    }

    /* General util functions */

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#encodeIdentifier}
     */
    protected CBORObject encodeIdentifier(byte[] identifier) {
        CBORObject identifierCBOR = null;

        if (identifier != null && identifier.length != 1) {
            // Encode the EDHOC connection identifier as a CBOR byte string
            identifierCBOR = CBORObject.FromObject(identifier);
        }

        if (identifier != null && identifier.length == 1) {
            int byteValue = EdhocUtil.bytesToInt(identifier);

            if ((byteValue >= 0 && byteValue <= 23) || (byteValue >= 32 && byteValue <= 55)) {
                // The EDHOC connection identifier is in the range 0x00-0x17 or in the range
                // 0x20-0x37.
                // That is, it happens to be the serialization of a CBOR integer with numeric
                // value -24..23

                // Encode the EDHOC connection identifier as a CBOR integer
                identifierCBOR = CBORObject.DecodeFromBytes(identifier);
            } else {
                // Encode the EDHOC connection identifier as a CBOR byte string
                identifierCBOR = CBORObject.FromObject(identifier);
            }
        }

        return identifierCBOR;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#decodeIdentifier}
     */
    protected byte[] decodeIdentifier(CBORObject identifierCbor) {
        byte[] identifier = null;

        if (identifierCbor != null && identifierCbor.getType() == CBORType.ByteString) {
            identifier = identifierCbor.GetByteString();

            // Consistency check
            if (identifier.length == 1) {
                int byteValue = EdhocUtil.bytesToInt(identifier);
                if ((byteValue >= 0 && byteValue <= 23) || (byteValue >= 32 && byteValue <= 55))
                    // This EDHOC connection identifier should have been encoded as a CBOR integer
                    identifier = null;
            }
        } else if (identifierCbor != null && identifierCbor.getType() == CBORType.Integer) {
            identifier = identifierCbor.EncodeToBytes();

            if (identifier.length != 1) {
                // This EDHOC connection identifier is not valid or was not encoded according to
                // deterministic CBOR
                identifier = null;
            }

        }
        return identifier;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeTH2}
     */
    protected byte[] computeTH2(String hashAlgorithm, byte[] gY, byte[] cR, byte[] hashMessage1) {
        int offset = 0;
        byte[] hashInput = new byte[gY.length + cR.length + hashMessage1.length];
        System.arraycopy(gY, 0, hashInput, offset, gY.length);
        offset += gY.length;
        System.arraycopy(cR, 0, hashInput, offset, cR.length);
        offset += cR.length;
        System.arraycopy(hashMessage1, 0, hashInput, offset, hashMessage1.length);

        try {
            return EdhocUtil.computeHash(hashInput, hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Invalid hash algorithm when computing TH2: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computePRK2e}
     */
    protected byte[] computePRK2e(byte[] th2, byte[] dhSecret, String hashAlgorithm) {
        if (hashAlgorithm.equals("SHA-256") || hashAlgorithm.equals("SHA-384") || hashAlgorithm.equals("SHA-512")) {
            try {
                return Hkdf.extract(th2, dhSecret);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                LOGGER.error("Generating PRK_2e: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computePRK3e2m}
     */
    protected byte[] computePRK3e2m(EdhocSessionPersistent session, byte[] prk2e, byte[] th2, OneKey peerLongTerm,
            OneKey peerEphemeral) {
        byte[] prk3e2m = null;
        int authenticationMethod = session.getMethod();
        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_2) {
            // The responder uses signatures as authentication method, then PRK_3e2m is
            // equal to PRK_2e
            prk3e2m = new byte[prk2e.length];
            System.arraycopy(prk2e, 0, prk3e2m, 0, prk2e.length);
        } else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_1
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The responder does not use signatures as authentication method, then PRK_3e2m
            // has to be computed
            byte[] dhSecret;
            OneKey privateKey;
            OneKey publicKey;

            if (session.isInitiator()) {
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

            if (!EdhocUtil.checkDiffieHellmanKeyAgainstCipherSuite(privateKey, selectedCipherSuite)) {
                LOGGER.error("Computing the Diffie-Hellman Secret (privateKey check)");
                return null;
            }

            if (!EdhocUtil.checkDiffieHellmanKeyAgainstCipherSuite(publicKey, selectedCipherSuite)) {
                LOGGER.error("Computing the Diffie-Hellman Secret (publicKey check)");
                return null;
            }

            dhSecret = SharedSecretCalculation.generateSharedSecret(privateKey, publicKey);

            if (dhSecret == null) {
                LOGGER.error("Computing the Diffie-Hellman Secret (generation)");
                return null;
            }

            LOGGER.debug(EdhocUtil.byteArrayToString("G_RX", dhSecret));

            String hashAlgorithm = EdhocSession.getEdhocHashAlg(selectedCipherSuite);

            // Compute SALT_3e2m
            byte[] salt3e2m;
            int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
            CBORObject context = CBORObject.FromObject(th2);

            try {
                salt3e2m = session.edhocKDF(prk2e, Constants.KDF_LABEL_SALT_3E2M, context, length);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                LOGGER.error("Generating SALT_3e2m: " + e.getMessage());
                return null;
            }

            LOGGER.debug(EdhocUtil.byteArrayToString("SALT_3e2m", salt3e2m));

            if (hashAlgorithm.equals("SHA-256") || hashAlgorithm.equals("SHA-384") || hashAlgorithm.equals("SHA-512")) {
                try {
                    prk3e2m = Hkdf.extract(salt3e2m, dhSecret);
                } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                    LOGGER.error("Generating PRK_3e2m: " + e.getMessage());
                    return null;
                }
            }
        }
        return prk3e2m;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeMAC2}
     */
    protected byte[] computeMAC2(EdhocSessionPersistent session, byte[] prk3e2m, byte[] th2,
            CBORObject cR, CBORObject idCredR, byte[] credR, CBORObject[] ead2) {

        // Build the CBOR sequence to use for 'context': ( ID_CRED_R, TH_2, CRED_R,
        // ?EAD_2 )
        // The actual 'context' is a CBOR byte string with value the serialization of
        // the CBOR sequence
        List<CBORObject> objectList = new ArrayList<>();
        if (!hasProtocolVersionLeqV22()) {
            objectList.add(cR);
        }
        objectList.add(idCredR);
        objectList.add(CBORObject.FromObject(th2));
        objectList.add(CBORObject.DecodeFromBytes(credR));

        if (ead2 != null && ead2.length > 0) {
            Collections.addAll(objectList, ead2);
        }

        byte[] contextSequence = EdhocUtil.buildCBORSequence(objectList);
        CBORObject context = CBORObject.FromObject(contextSequence);
        LOGGER.debug(EdhocUtil.byteArrayToString("context_2", contextSequence));

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
            LOGGER.error("Computing MAC_2: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeExternalData}
     */
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
            byte[] eadSequence = EdhocUtil.buildCBORSequence(objectList);

            externalDataList.add(CBORObject.FromObject(eadSequence));
        }

        return EdhocUtil.concatenateByteArrays(externalDataList);
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeSignatureOrMac2}
     */
    protected byte[] computeSignatureOrMac2(EdhocSessionPersistent session, byte[] mac2, byte[] externalData) {
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
                if (!EdhocUtil.checkSignatureKeyAgainstCipherSuite(identityKey, selectedCipherSuite)) {
                    LOGGER.error("Signing MAC_2 to produce Signature_or_MAC_2 (signature key check)");
                    return null;
                }

                LOGGER.debug(
                        EdhocUtil.byteArrayToString("External Data for signing MAC_2 to produce Signature_or_MAC_2",
                                externalData));

                signatureOrMac2 = EdhocUtil.computeSignature(session.getIdCred(), externalData, mac2, identityKey);

            } catch (CoseException e) {
                LOGGER.error("Signing MAC_2 to produce Signature_or_MAC_2: " + e.getMessage());
                return null;
            }
        }

        return signatureOrMac2;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeKeystream2}
     */
    protected byte[] computeKeystream2(EdhocSessionPersistent session, byte[] th2, byte[] prk2e, int length) {
        CBORObject context = CBORObject.FromObject(th2);
        int selectedCipherSuite = session.getSelectedCipherSuite();
        String hashAlg = EdhocSession.getEdhocHashAlg(selectedCipherSuite);
        int hashLength = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
        byte[] keystream2;

        if ((!hashAlg.equals("SHA-256") && !hashAlg.equals("SHA-384") && !hashAlg.equals("SHA-512"))
                || (length <= 255 * hashLength)) {
            try {
                keystream2 = session.edhocKDF(prk2e, Constants.KDF_LABEL_KEYSTREAM_2, context, length);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                LOGGER.error("Generating KEYSTREAM_2 (whole): " + e.getMessage());
                return null;
            }
        } else {
            byte[] part;
            int regularPartSize = 255 * hashLength;
            int lastPartSize = regularPartSize;

            int numParts = length / regularPartSize;
            if ((length % regularPartSize) != 0) {
                lastPartSize = length % regularPartSize;
                numParts++;
            }

            int offset = 0;
            keystream2 = new byte[length];
            for (int i = 0; i < numParts; i++) {
                int numBytes = (i == (numParts - 1)) ? lastPartSize : regularPartSize;

                try {
                    part = session.edhocKDF(prk2e, -i, context, numBytes);
                } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                    LOGGER.error("Generating KEYSTREAM_2 (partial): " + e.getMessage());
                    return null;
                }

                System.arraycopy(part, 0, keystream2, offset, part.length);
                offset += part.length;
            }
        }

        return keystream2;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#verifySignatureOrMac2}
     */
    protected boolean verifySignatureOrMac2(EdhocSessionPersistent session, OneKey peerLongTerm, CBORObject peerIdCred,
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
            if (!EdhocUtil.checkSignatureKeyAgainstCipherSuite(peerLongTerm, selectedCipherSuite)) {
                LOGGER.error("Verifying the signature of Signature_or_MAC_2 (signature check)");
                return false;
            }

            try {
                return EdhocUtil.verifySignature(signatureOrMac2, peerIdCred, externalData, mac2, peerLongTerm);
            } catch (CoseException e) {
                LOGGER.error("Verifying the signature of Signature_or_MAC_2: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeTH3}
     */
    protected byte[] computeTH3(String hashAlgorithm, byte[] th2, byte[] plaintext2, byte[] credR) {
        int inputLength = th2.length + plaintext2.length + credR.length;
        int offset = 0;
        byte[] hashInput = new byte[inputLength];
        System.arraycopy(th2, 0, hashInput, offset, th2.length);
        offset += th2.length;
        System.arraycopy(plaintext2, 0, hashInput, offset, plaintext2.length);
        offset += plaintext2.length;
        System.arraycopy(credR, 0, hashInput, offset, credR.length);

        try {
            return EdhocUtil.computeHash(hashInput, hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Invalid hash algorithm when computing TH3: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computePRK4e3m}
     */
    protected byte[] computePRK4e3m(EdhocSessionPersistent session, byte[] prk3e2m, byte[] th3, OneKey peerLongTerm,
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

            if (!EdhocUtil.checkDiffieHellmanKeyAgainstCipherSuite(privateKey, selectedCipherSuite)) {
                LOGGER.error("Computing the Diffie-Hellman Secret (privateKey check)");
                return null;
            }

            if (!EdhocUtil.checkDiffieHellmanKeyAgainstCipherSuite(publicKey, selectedCipherSuite)) {
                LOGGER.error("Computing the Diffie-Hellman Secret (publicKey check)");
                return null;
            }

            dhSecret = SharedSecretCalculation.generateSharedSecret(privateKey, publicKey);

            if (dhSecret == null) {
                LOGGER.error("Computing the Diffie-Hellman Secret");
                return null;
            }

            LOGGER.debug(EdhocUtil.byteArrayToString("G_IY", dhSecret));

            String hashAlgorithm = EdhocSession.getEdhocHashAlg(session.getSelectedCipherSuite());

            // Compute SALT_4e3m
            byte[] salt4e3m;
            int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
            CBORObject context = CBORObject.FromObject(th3);

            try {
                salt4e3m = session.edhocKDF(prk3e2m, Constants.KDF_LABEL_SALT_4E3M, context, length);
            } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                LOGGER.error("Generating SALT_4e3m: " + e.getMessage());
                return null;
            }

            if (salt4e3m == null) {
                LOGGER.error("Computing SALT_4e3m");
                return null;
            }

            LOGGER.debug(EdhocUtil.byteArrayToString("SALT_4e3m", salt4e3m));

            if (hashAlgorithm.equals("SHA-256") || hashAlgorithm.equals("SHA-384") || hashAlgorithm.equals("SHA-512")) {
                try {
                    prk4e3m = Hkdf.extract(salt4e3m, dhSecret);
                } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                    LOGGER.error("Generating PRK_4e3m: " + e.getMessage());
                    return null;
                }
            }
        }

        return prk4e3m;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeMAC3}
     */
    protected byte[] computeMAC3(EdhocSessionPersistent session, byte[] prk4e3m, byte[] th3, CBORObject idCredI,
            byte[] credI, CBORObject[] ead3) {

        // Build the CBOR sequence for 'context': ( ID_CRED_I, TH_3, CRED_I, ?EAD_3 )
        // The actual 'context' is a CBOR byte string with value the serialization of
        // the CBOR sequence
        List<CBORObject> objectList = new ArrayList<>();
        objectList.add(idCredI);
        objectList.add(CBORObject.FromObject(th3));
        objectList.add(CBORObject.DecodeFromBytes(credI));

        if (ead3 != null && ead3.length > 0) {
            Collections.addAll(objectList, ead3);
        }
        byte[] contextSequence = EdhocUtil.buildCBORSequence(objectList);
        CBORObject context = CBORObject.FromObject(contextSequence);
        LOGGER.debug(EdhocUtil.byteArrayToString("context_3", contextSequence));

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
            LOGGER.error("Computing MAC_3: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeSignatureOrMac3}
     */
    protected byte[] computeSignatureOrMac3(EdhocSessionPersistent session, byte[] mac3, byte[] externalData) {
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
                if (!EdhocUtil.checkSignatureKeyAgainstCipherSuite(identityKey, selectedCipherSuite)) {
                    LOGGER.error("Signing MAC_3 to produce Signature_or_MAC_3 (signature key check)");
                    return null;
                }

                LOGGER.debug(
                        EdhocUtil.byteArrayToString("External Data for signing MAC_3 to produce Signature_or_MAC_3",
                                externalData));

                signatureOrMac3 = EdhocUtil.computeSignature(session.getIdCred(), externalData, mac3, identityKey);

            } catch (CoseException e) {
                LOGGER.error("Signing MAC_3 to produce Signature_or_MAC_3: " + e.getMessage());
                return null;
            }
        }

        return signatureOrMac3;

    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey}
     * and {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey}
     */
    protected byte[] computeKeyOrIV3(String keyName, EdhocSessionPersistent session, byte[] th3, byte[] prk3e2m) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        CBORObject context = CBORObject.FromObject(th3);

        String name;
        int length;
        int label;

        switch (keyName) {
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
            LOGGER.error("Generating {}\n{}", name, e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeCiphertext3}
     */
    protected byte[] computeCiphertext3(int selectedCipherSuite, byte[] externalData, byte[] plaintext, byte[] k3ae,
            byte[] iv3ae) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return EdhocUtil.encrypt(emptyMap, externalData, plaintext, alg, iv3ae, k3ae);
        } catch (CoseException e) {
            LOGGER.error("Computing CIPHERTEXT_3: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeTH4}
     */
    protected byte[] computeTH4(String hashAlgorithm, byte[] th3, byte[] plaintext3, byte[] credI) {
        int inputLength = th3.length + plaintext3.length + credI.length;
        int offset = 0;
        byte[] hashInput = new byte[inputLength];
        System.arraycopy(th3, 0, hashInput, offset, th3.length);
        offset += th3.length;
        System.arraycopy(plaintext3, 0, hashInput, offset, plaintext3.length);
        offset += plaintext3.length;
        System.arraycopy(credI, 0, hashInput, offset, credI.length);

        try {
            return EdhocUtil.computeHash(hashInput, hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Invalid hash algorithm when computing TH4: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computePRKout}
     */
    protected byte[] computePRKout(EdhocSessionPersistent session, byte[] th4, byte[] prk4e3m) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
        CBORObject context = CBORObject.FromObject(th4);

        try {
            return session.edhocKDF(prk4e3m, Constants.KDF_LABEL_PRK_OUT, context, length);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.error("Generating PRK_out: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computePRKexporter}
     */
    protected byte[] computePRKexporter(EdhocSessionPersistent session, byte[] prkOut) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        int length = EdhocSession.getEdhocHashAlgOutputSize(selectedCipherSuite);
        CBORObject context = CBORObject.FromObject(new byte[0]);

        try {
            return session.edhocKDF(prkOut, Constants.KDF_LABEL_PRK_EXPORTER, context, length);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            LOGGER.error("Generating PRK_exporter: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#decryptCiphertext3}
     */
    protected byte[] decryptCiphertext3(int selectedCipherSuite, byte[] externalData, byte[] ciphertext, byte[] k3ae,
            byte[] iv3ae) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return EdhocUtil.decrypt(emptyMap, externalData, ciphertext, alg, iv3ae, k3ae);
        } catch (CoseException e) {
            LOGGER.error("Decrypting CIPHERTEXT_3: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#verifySignatureOrMac3}
     */
    protected boolean verifySignatureOrMac3(EdhocSessionPersistent session, OneKey peerLongTerm, CBORObject peerIdCred,
            byte[] signatureOrMac3, byte[] externalData, byte[] mac3) {
        // Used by Responder
        int authenticationMethod = session.getMethod();

        if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_2
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_3) {
            // The initiator does not use signatures as authentication method,
            // then Signature_or_MAC_3 has to be equal to MAC_3
            return Arrays.equals(signatureOrMac3, mac3);
        } else if (authenticationMethod == Constants.EDHOC_AUTH_METHOD_0
                || authenticationMethod == Constants.EDHOC_AUTH_METHOD_1) {
            // The initiator uses signatures as authentication method,
            // then Signature_or_MAC_3 is a signature to verify
            int selectedCipherSuite = session.getSelectedCipherSuite();

            // Consistency check of key type and curve against the selected cipher suite
            if (!EdhocUtil.checkSignatureKeyAgainstCipherSuite(peerLongTerm, selectedCipherSuite)) {
                LOGGER.error("Verifying the signature of Signature_or_MAC_3");
                return false;
            }

            try {
                return EdhocUtil.verifySignature(signatureOrMac3, peerIdCred, externalData, mac3, peerLongTerm);
            } catch (CoseException e) {
                LOGGER.error("Verifying the signature of Signature_or_MAC_3: " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey}
     * and {@link org.eclipse.californium.edhoc.MessageProcessor#computeKey}
     */
    protected byte[] computeKeyOrIV4(String keyName, EdhocSessionPersistent session, byte[] th4, byte[] prk4e3m) {
        int selectedCipherSuite = session.getSelectedCipherSuite();
        CBORObject context = CBORObject.FromObject(th4);

        String name;
        int length;
        int label;

        switch (keyName) {
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
            LOGGER.error("Generating {}\n{}", name, e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#computeCiphertext4}
     */
    protected byte[] computeCiphertext4(int selectedCipherSuite, byte[] externalData, byte[] plaintext, byte[] k4m,
            byte[] iv4m) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return EdhocUtil.encrypt(emptyMap, externalData, plaintext, alg, iv4m, k4m);
        } catch (CoseException e) {
            LOGGER.error("Computing CIPHERTEXT_4: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#decryptCiphertext4}
     */
    protected byte[] decryptCiphertext4(int selectedCipherSuite, byte[] externalData, byte[] ciphertext,
            byte[] k4ae, byte[] iv4ae) {
        AlgorithmID alg = EdhocSession.getEdhocAEADAlg(selectedCipherSuite);

        // Prepare the empty content for the COSE protected header
        CBORObject emptyMap = CBORObject.NewMap();

        try {
            return EdhocUtil.decrypt(emptyMap, externalData, ciphertext, alg, iv4ae, k4ae);
        } catch (CoseException e) {
            LOGGER.error("Decrypting CIPHERTEXT_4: " + e.getMessage());
            return null;
        }
    }

    protected boolean hasProtocolVersionLeqV15() {
        return switch (edhocMapperState.getProtocolVersion()) {
            case v14, v15 -> true;
            default -> false;
        };
    }

    protected boolean hasProtocolVersionLeqV17() {
        return hasProtocolVersionLeqV15() || switch (edhocMapperState.getProtocolVersion()) {
            case v16, v17 -> true;
            default -> false;
        };
    }

    protected boolean hasProtocolVersionLeqV19() {
        return hasProtocolVersionLeqV17() || switch (edhocMapperState.getProtocolVersion()) {
            case v18, v19 -> true;
            default -> false;
        };
    }

    protected boolean hasProtocolVersionLeqV22() {
        return hasProtocolVersionLeqV19() || switch (edhocMapperState.getProtocolVersion()) {
            case v20, v21, v22 -> true;
            default -> false;
        };
    }

    protected CBORObject[] preParseEADleqV17(CBORObject[] objectList, int baseIndex, Set<Integer> supportedEADs) {
        int length = objectList.length - baseIndex;
        CBORObject[] eadArray = new CBORObject[length];

        if (length < 0) {
            LOGGER.error("Provided baseIndex greater than the length of objectList");
            return null;
        }

        if ((length & 1) == 1) { // if odd length
            LOGGER.error("EAD items should have even length, but found length " + length);
            return null;
        }

        int eadIndex = 0;

        for (int i = baseIndex; i < objectList.length; i++) {

            // The first element of each pair is an ead_label, and must be a non-zero CBOR
            // integer
            if ((eadIndex & 1) == 0) {
                if (objectList[i].getType() != CBORType.Integer || objectList[i].AsInt32() == 0) {
                    LOGGER.error("Malformed or Invalid EAD label");
                    return null;
                }

                int eadLabel = objectList[i].AsInt32();
                if (eadLabel < 0 && !supportedEADs.contains(eadLabel)) {
                    // The EAD item is critical and is not supported
                    LOGGER.error("Unsupported EAD critical item with ead_label: " + eadLabel);
                    return null;
                }

                // Move on to the ead_value
                eadIndex++;
                continue;
            }

            // The second element of each pair is an ead_value, and must be a CBOR byte
            // string
            if ((eadIndex & 1) == 1) {
                if (objectList[i].getType() != CBORType.ByteString) {
                    LOGGER.error("Malformed or invalid EAD value");
                    return null;
                }

                // Skip this EAD item as not supported and not critical
                if (!supportedEADs.contains(eadIndex - 1)) {
                    eadIndex++;
                    continue;
                }

                // This EAD item is supported, so it is kept for further processing

                // Make a hard copy of ead_label
                byte[] serializedObjectLabel = objectList[i - 1].EncodeToBytes();
                CBORObject elementLabel = CBORObject.DecodeFromBytes(serializedObjectLabel);
                eadArray[eadIndex - 1] = elementLabel;

                // Make a hard copy of ead_value
                byte[] serializedObjectValue = objectList[i].EncodeToBytes();
                CBORObject elementValue = CBORObject.DecodeFromBytes(serializedObjectValue);
                eadArray[eadIndex] = elementValue;

                eadIndex++;
            }
        }

        return eadArray;
    }

    /**
     * Adapted from
     * {@link org.eclipse.californium.edhoc.MessageProcessor#preParseEAD}
     */
    protected CBORObject[] preParseEAD(CBORObject[] objectList, int baseIndex, int msgNum, Set<Integer> supportedEADs) {
        int length = objectList.length - baseIndex;
        CBORObject[] eadArray = new CBORObject[length];
        int eadIndex = 0;

        if (length < 0) {
            LOGGER.error("Provided baseIndex greater than the length of objectList");
            return null;
        }

        // The actual goal of each step is to go through one EAD item
        // At each step, the element with index 'i' must be an ead_label
        // For EAD items that are supported or non-critical, the corresponding ead_value
        // (if present) is
        // handled during the same step, so that the next step will consider the next
        // EAD item, if any
        for (int i = baseIndex; i < objectList.length; i++) {
            CBORObject currObject = objectList[i];

            if (currObject.getType() != CBORType.Integer) {
                // Each EAD item must start with a CBOR integer encoding the ead_label
                LOGGER.error("Malformed or invalid EAD_" + msgNum);
                return null;
            }

            if (i + 1 < objectList.length
                    && objectList[i + 1].getType() != CBORType.Integer
                    && objectList[i + 1].getType() != CBORType.ByteString) {

                // The immediately following item in the CBOR sequence (if any) must be a CBOR
                // integer or a CBOR byte string
                LOGGER.error("Malformed or invalid EAD_" + msgNum);
                return null;
            }

            int eadLabel = currObject.AsInt32();

            if (eadLabel == Constants.EAD_LABEL_PADDING) {
                // This is the padding EAD item and it is not passed to the application for
                // further processing
                LOGGER.debug("EAD Label: {}", eadLabel);

                if (i + 1 < objectList.length && objectList[i + 1].getType() == CBORType.ByteString) {
                    LOGGER.debug(EdhocUtil.byteArrayToString("EAD Value", objectList[i + 1].GetByteString()));
                    // Skip the corresponding ead_value, if present
                    i++;
                }
                continue;
            }

            int eadLabelUnsigned = Math.abs(eadLabel);
            if (!supportedEADs.contains(eadLabelUnsigned)) {
                if (eadLabel < 0) {
                    // The EAD item is critical but not supported
                    LOGGER.error("Unsupported EAD_" + msgNum + " critical item with ead_label " + eadLabelUnsigned);
                    return null;
                }

                // The EAD item is non-critical and not supported,
                // it is not passed to the application for further processing
                if (i + 1 < objectList.length && objectList[i + 1].getType() == CBORType.ByteString) {
                    // This will result in moving to the next EAD item, if any
                    i++;
                }
                continue;
            }

            // This EAD item is supported, so it is kept for further processing

            // Make a hard copy of the ead_label
            byte[] serializedObjectLabel = currObject.EncodeToBytes();
            CBORObject elementLabel = CBORObject.DecodeFromBytes(serializedObjectLabel);
            eadArray[eadIndex] = elementLabel;
            eadIndex++;

            LOGGER.debug("EAD Label: {}", eadLabel);
            // Make a hard copy of the ead_value, if present
            if (i + 1 < objectList.length && objectList[i + 1].getType() == CBORType.ByteString) {
                byte[] serializedObjectValue = objectList[i + 1].EncodeToBytes();
                CBORObject elementValue = CBORObject.DecodeFromBytes(serializedObjectValue);
                eadArray[eadIndex] = elementValue;
                eadIndex++;

                LOGGER.debug(EdhocUtil.byteArrayToString("EAD Value", objectList[i + 1].GetByteString()));
                // This will result in moving to the next EAD item, if any
                i++;
            }
        }

        // Prepare the subset of the EAD items to provide to the application for further
        // processing
        CBORObject[] ret = new CBORObject[eadIndex];
        for (int i = 0; i < eadIndex; i++) {
            ret[i] = eadArray[i];
        }
        return ret;
    }
}
