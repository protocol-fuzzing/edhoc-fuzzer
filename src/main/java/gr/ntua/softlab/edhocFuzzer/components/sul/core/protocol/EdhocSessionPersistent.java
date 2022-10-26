package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.CoapExchanger;
import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.*;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class EdhocSessionPersistent extends EdhocSession {

    protected String sessionUri;

    // object containing queues that contain objects with info about exchanges
    // (typically only last exchange) regarding this session
    protected CoapExchanger coapExchanger;

    protected String oscoreUri;
    protected int oscoreReplayWindow;
    protected int oscoreMaxUnfragmentedSize;
    protected boolean oscoreCtxGenerated;

    protected CBORObject cipherSuitesIncludeInError;

    protected CBORObject[] ead1;
    protected CBORObject[] ead2;
    protected CBORObject[] ead3;
    protected CBORObject[] ead4;

    protected boolean sessionResetEnabled;

    protected byte[] forceOscoreSenderId;
    protected byte[] forceOscoreRecipientId;

    public EdhocSessionPersistent(
            String sessionUri, boolean initiator, boolean clientInitiated, int method, byte[] connectionId,
            EdhocEndpointInfoPersistent edhocEndpointInfoPersistent, HashMapCtxDB oscoreDB,
            CoapExchanger coapExchanger, boolean sessionResetEnabled, byte[] forceOscoreSenderId,
            byte[] forceOscoreRecipientId) {

        super(initiator, clientInitiated, method, connectionId,
                edhocEndpointInfoPersistent.getKeyPairs(), edhocEndpointInfoPersistent.getIdCreds(),
                edhocEndpointInfoPersistent.getCreds(), edhocEndpointInfoPersistent.getSupportedCipherSuites(),
                edhocEndpointInfoPersistent.getAppProfiles().get(sessionUri),
                edhocEndpointInfoPersistent.getEdp(), oscoreDB);

        this.sessionUri = sessionUri;
        this.oscoreUri = edhocEndpointInfoPersistent.getOscoreUri();
        this.oscoreReplayWindow = edhocEndpointInfoPersistent.getOscoreReplayWindow();
        this.oscoreMaxUnfragmentedSize = edhocEndpointInfoPersistent.getOscoreMaxUnfragmentedSize();
        this.coapExchanger = coapExchanger;
        this.sessionResetEnabled = sessionResetEnabled;
        this.forceOscoreSenderId = forceOscoreSenderId;
        this.forceOscoreRecipientId = forceOscoreRecipientId;

        reset();
    }

    public void resetIfEnabled() {
        if (sessionResetEnabled) {
            reset();
        } else {
            // do not reset, but allow for new oscore context to be derived
            oscoreCtxGenerated = false;
        }
    }

    public void reset() {
        // own info with first supported cipher suite
        int selectedCipherSuite = getSupportedCipherSuites().get(0);
        setSelectedCipherSuite(selectedCipherSuite);
        setAuthenticationCredential();
        setEphemeralKey();

        // peer dummy info
        setPeerConnectionId(new byte[]{0, 0, 0, 0});
        setPeerIdCred(CBORObject.Null);
        setPeerCred(new byte[]{0, 0, 0, 0});
        setPeerLongTermPublicKey(new OneKey());

        // dummy peerEphemeralPublicKey same as own ephemeral key
        // in order for the two keys to be of the same curve
        setPeerEphemeralPublicKey(getEphemeralKey());

        // message1 hash
        setHashMessage1(new byte[]{1});

        // plaintext2
        setPlaintext2(new byte[]{1});

        // inner key-derivation Keys
        setPRK2e(new byte[]{1});
        setPRK3e2m(new byte[]{1});
        setPRK4e3m(new byte[]{1});

        // transcript hashes
        setTH2(new byte[]{1});
        setTH3(new byte[]{1});
        setTH4(new byte[]{1});

        // key after successful EDHOC execution
        setPRKout(new byte[]{1});
        setPRKexporter(new byte[]{1});

        // message3, to be used for building an EDHOC+OSCORE request
        setMessage3(new byte[]{1});

        // eads
        this.ead1 = null;
        this.ead2 = null;
        this.ead3 = null;
        this.ead4 = null;

        // setup dummy oscore context and reset flag
        oscoreCtxGenerated = false;
        setupOscoreContext();
        oscoreCtxGenerated = false;
    }

    public void setupOscoreContext() {
        if (oscoreCtxGenerated) {
            // oscore context is derived only it is not already derived in this session.
            // In case it is already derived, then the current context is active
            return;
        }

        /* Invoke the EDHOC-Exporter to produce OSCORE input material */
        byte[] masterSecret = getMasterSecretOSCORE(this);
        byte[] masterSalt = getMasterSaltOSCORE(this);

        /* Set up the OSCORE Security Context */

        // The Sender ID of this peer is the EDHOC connection identifier of the other peer normally
        // or the forced sender id from the input
        byte[] senderId = forceOscoreSenderId != null ? forceOscoreSenderId : getPeerConnectionId();
        // The Recipient ID of this peer is the EDHOC connection identifier of this peer normally
        // or the forced recipient id from the input
        byte[] recipientId = forceOscoreRecipientId != null ? forceOscoreRecipientId : getConnectionId();

        int selectedCipherSuite = getSelectedCipherSuite();
        AlgorithmID alg = getAppAEAD(selectedCipherSuite);
        AlgorithmID hkdf = getAppHkdf(selectedCipherSuite);

        OSCoreCtx ctx;
        if (Arrays.equals(senderId, recipientId)) {
            throw new RuntimeException("Error: the Sender ID coincides with the Recipient ID");
        }

        try {
            ctx = new OSCoreCtx(masterSecret, true, alg, senderId, recipientId, hkdf,
                    oscoreReplayWindow, masterSalt, null, oscoreMaxUnfragmentedSize);
        } catch (OSException e) {
            throw new RuntimeException("Error when deriving the OSCORE Security Context: " + e.getMessage());
        }

        try {
            getOscoreDb().addContext(oscoreUri, ctx);
        } catch (OSException e) {
            throw new RuntimeException("Error when adding the OSCORE Security Context to the context database: "
                    + e.getMessage());
        }

        oscoreCtxGenerated = true;
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
    public byte[] edhocExporter(int label, CBORObject context, int len) throws InvalidKeyException, NoSuchAlgorithmException {
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

    public CBORObject getCipherSuitesIncludeInError() {
        return cipherSuitesIncludeInError;
    }

    public void setCipherSuitesIncludeInError(CBORObject cipherSuitesIncludeInError) {
        this.cipherSuitesIncludeInError = cipherSuitesIncludeInError;
    }

    public CBORObject[] getEad1() {
        return ead1;
    }

    public void setEad1(CBORObject[] ead1) {
        this.ead1 = ead1;
    }

    public CBORObject[] getEad2() {
        return ead2;
    }

    public void setEad2(CBORObject[] ead2) {
        this.ead2 = ead2;
    }

    public CBORObject[] getEad3() {
        return ead3;
    }

    public void setEad3(CBORObject[] ead3) {
        this.ead3 = ead3;
    }

    public CBORObject[] getEad4() {
        return ead4;
    }

    public void setEad4(CBORObject[] ead4) {
        this.ead4 = ead4;
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
}
