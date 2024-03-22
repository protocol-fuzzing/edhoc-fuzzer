package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.CoapExchanger;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.edhoc.EdhocSession;
import org.eclipse.californium.edhoc.SideProcessor;
import org.eclipse.californium.edhoc.Util;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class EdhocSessionPersistent extends EdhocSession {

    private static final Logger LOGGER = LogManager.getLogger();

    protected String sessionUri;

    // object containing queues that contain objects with info about exchanges
    // (typically only last exchange) regarding this session
    protected CoapExchanger coapExchanger;

    protected String oscoreUri;
    protected int oscoreReplayWindow;
    protected int oscoreMaxUnfragmentedSize;
    protected boolean oscoreCtxGenerated;

    protected List<Integer> peerSupportedCipherSuites;
    protected CBORObject cipherSuitesIncludeInError;

    protected boolean sessionResetEnabled;

    protected byte[] forceOscoreSenderId;
    protected byte[] forceOscoreRecipientId;

    protected byte[] connectionId;

    public EdhocSessionPersistent(
            String sessionUri, boolean initiator, boolean clientInitiated, int method, byte[] connectionId,
            EdhocEndpointInfoPersistent edhocEndpointInfoPersistent, List<Integer> peerSupportedCipherSuites,
            HashMapCtxDB oscoreDB, CoapExchanger coapExchanger, boolean sessionResetEnabled,
            byte[] forceOscoreSenderId, byte[] forceOscoreRecipientId) {

        super(initiator, clientInitiated, method, connectionId, edhocEndpointInfoPersistent.getKeyPairs(),
                edhocEndpointInfoPersistent.getIdCreds(), edhocEndpointInfoPersistent.getCreds(),
                edhocEndpointInfoPersistent.getSupportedCipherSuites(), peerSupportedCipherSuites,
                edhocEndpointInfoPersistent.getSupportedEADs(),
                edhocEndpointInfoPersistent.getAppProfiles().get(sessionUri),
                edhocEndpointInfoPersistent.getTrustModel(), oscoreDB);

        this.sessionUri = sessionUri;
        this.oscoreUri = edhocEndpointInfoPersistent.getOscoreUri();
        this.oscoreReplayWindow = edhocEndpointInfoPersistent.getOscoreReplayWindow();
        this.oscoreMaxUnfragmentedSize = edhocEndpointInfoPersistent.getOscoreMaxUnfragmentedSize();
        this.peerSupportedCipherSuites = peerSupportedCipherSuites;
        this.coapExchanger = coapExchanger;
        this.sessionResetEnabled = sessionResetEnabled;
        this.forceOscoreSenderId = forceOscoreSenderId;
        this.forceOscoreRecipientId = forceOscoreRecipientId;
        this.connectionId = connectionId;

        SideProcessor sideProcessor = new SideProcessor(
                edhocEndpointInfoPersistent.getTrustModel(),
                edhocEndpointInfoPersistent.getPeerCredentials(),
                edhocEndpointInfoPersistent.getEadProductionInput());
        sideProcessor.setEdhocSession(this);
        this.setSideProcessor(sideProcessor);

        reset();
    }

    public synchronized void resetIfEnabled() {
        if (sessionResetEnabled) {
            reset();
        } else {
            // do not reset, but allow for new oscore context to be derived
            oscoreCtxGenerated = false;
        }
    }

    public synchronized void reset() {
        // own info with first supported cipher suite
        int selectedCipherSuite = getSupportedCipherSuites().get(0);
        setSelectedCipherSuite(selectedCipherSuite);
        setAuthenticationCredential();
        setEphemeralKey();

        // peer dummy info
        setPeerConnectionId(new byte[] { 0, 0, 0, 0 });
        setPeerCred(new byte[] { 0, 0, 0, 0 });
        setPeerIdCred(Util.buildIdCredKid(getPeerCred()));

        // in order for the ephemeral and long-term keys of the two peers to be of the
        // same curve
        // dummy peerEphemeralPublicKey same as own ephemeral key
        // dummy peerLongTermPublicKey same as own long-term key
        setPeerEphemeralPublicKey(getEphemeralKey());
        setPeerLongTermPublicKey(getKeyPair());

        // message1 hash
        setHashMessage1(new byte[] { 1 });

        // plaintext2
        setPlaintext2(new byte[] { 1 });

        // inner key-derivation Keys
        setPRK2e(new byte[] { 1 });
        setPRK3e2m(new byte[] { 1 });
        setPRK4e3m(new byte[] { 1 });

        // transcript hashes
        setTH2(new byte[] { 1 });
        setTH3(new byte[] { 1 });
        setTH4(new byte[] { 1 });

        // key after successful EDHOC execution
        setPRKout(new byte[] { 1 });
        setPRKexporter(new byte[] { 1 });

        // message3, to be used for building an EDHOC+OSCORE request
        setMessage3(new byte[] { 1 });

        // setup dummy oscore context and reset flag
        oscoreCtxGenerated = false;
        setupOscoreContext();
        oscoreCtxGenerated = false;
    }

    public synchronized void setupOscoreContext() {
        if (oscoreCtxGenerated) {
            // oscore context is derived only if not already derived in this session
            // in case it is already derived, then the current context is active
            return;
        }

        /* Invoke the EDHOC-Exporter to produce OSCORE input material */
        byte[] masterSecret = getMasterSecretOSCORE(this);
        LOGGER.debug(EdhocUtil.byteArrayToString("OSCORE Master Secret", masterSecret));

        byte[] masterSalt = getMasterSaltOSCORE(this);
        LOGGER.debug(EdhocUtil.byteArrayToString("OSCORE Master Salt", masterSalt));

        /* Set up the OSCORE Security Context */

        byte[] senderId = getOscoreSenderId();
        LOGGER.debug(EdhocUtil.byteArrayToString("OSCORE Sender Id", senderId));
        LOGGER.debug(EdhocUtil.byteArrayToString("peerConnectionId", getPeerConnectionId()));
        LOGGER.debug(EdhocUtil.byteArrayToString("forceOscoreSenderId", forceOscoreSenderId));

        byte[] recipientId = getOscoreRecipientId();
        LOGGER.debug(EdhocUtil.byteArrayToString("OSCORE Recipient Id", recipientId));
        LOGGER.debug(EdhocUtil.byteArrayToString("connectionId", getConnectionId()));
        LOGGER.debug(EdhocUtil.byteArrayToString("forceOscoreRecipientId", forceOscoreRecipientId));

        int selectedCipherSuite = getSelectedCipherSuite();
        AlgorithmID alg = getAppAEAD(selectedCipherSuite);
        AlgorithmID hkdf = getAppHkdf(selectedCipherSuite);

        if (Arrays.equals(senderId, recipientId)) {
            LOGGER.warn("The Sender ID will be equal to the Recipient ID in this OSCORE Security Context");
        }

        try {
            OSCoreCtx ctx = new OSCoreCtx(masterSecret, true, alg, senderId, recipientId, hkdf,
                    oscoreReplayWindow, masterSalt, null, oscoreMaxUnfragmentedSize);

            getOscoreDb().addContext(oscoreUri, ctx);
            oscoreCtxGenerated = true;
        } catch (OSException e) {
            oscoreCtxGenerated = false;
            LOGGER.error("Error when setting up the OSCORE Security Context: " + e.getMessage());
        } finally {
            notifyAll();
        }
    }

    public synchronized void waitForOscoreContext(long timeoutMillis) {
        LOGGER.debug("Start of waitForOscoreContext");
        if (!oscoreCtxGenerated) {
            try {
                wait(timeoutMillis);
            } catch (InterruptedException e) {
                LOGGER.warn("Wait for OSCORE context generation interrupted: {}", e.getMessage());
            }
        }
        // wait finished
        LOGGER.debug("End of waitForOscoreContext, OSCORE context generated: {}", oscoreCtxGenerated);
    }

    public byte[] getOscoreSenderId() {
        // the sender id of this peer is the forced sender id from the input
        // or the connection id of the other peer as expected
        return forceOscoreSenderId != null ? forceOscoreSenderId : getPeerConnectionId();
    }

    public byte[] getOscoreRecipientId() {
        // the recipient id of this peer is the forced recipient id from the input
        // or the connection id of this peer as expected
        return forceOscoreRecipientId != null ? forceOscoreRecipientId : getConnectionId();
    }

    @Override
    public void deleteTemporaryMaterial() {
        // do not delete anything
    }

    @Override
    public void cleanMessage1() {
        // do not clean anything
    }

    @Override
    public void cleanMessage3() {
        // do not clean anything
    }

    @Override
    public byte[] edhocExporter(int label, CBORObject context, int len)
            throws InvalidKeyException, NoSuchAlgorithmException {
        if (label < 0 || context.getType() != CBORType.ByteString || len < 0)
            return null;
        // do not check for session currentStep
        return edhocKDF(getPRKexporter(), label, context, len);
    }

    public String getSessionUri() {
        return sessionUri;
    }

    public CoapExchanger getCoapExchanger() {
        return coapExchanger;
    }

    @Override
    public List<Integer> getPeerSupportedCipherSuites() {
        return peerSupportedCipherSuites;
    }

    // If an error message carrying the peer supported cipher suites
    // is received, they can be stored in this session
    public void setPeerSupportedCipherSuites(List<Integer> peerSupportedCipherSuites) {
        this.peerSupportedCipherSuites = peerSupportedCipherSuites;
    }

    public CBORObject getCipherSuitesIncludeInError() {
        return cipherSuitesIncludeInError;
    }

    public void setCipherSuitesIncludeInError(CBORObject cipherSuitesIncludeInError) {
        this.cipherSuitesIncludeInError = cipherSuitesIncludeInError;
    }

    public boolean isSessionResetEnabled() {
        return sessionResetEnabled;
    }

    public byte[] getForceOscoreSenderId() {
        return forceOscoreSenderId;
    }

    public byte[] getForceOscoreRecipientId() {
        return forceOscoreRecipientId;
    }

    public void setConnectionId(byte[] id) {
        this.connectionId = id;
    }

    /**
     * Note: Override to enable modification of connectionId.
     * It is private and (in practice) immutable in the parent class EdhocSession.
     */
    @Override
    public byte[] getConnectionId() {
        return this.connectionId;
    }
}
