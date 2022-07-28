package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.edhoc.*;
import org.eclipse.californium.oscore.HashMapCtxDB;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

public class EdhocSessionPersistent extends EdhocSession {
    protected CBORObject[] ead1;
    protected CBORObject[] ead2;
    protected CBORObject[] ead3;
    protected CBORObject[] ead4;

    public EdhocSessionPersistent(
            boolean initiator, boolean clientInitiated, int method, byte[] connectionId,
            HashMap<Integer, HashMap<Integer, OneKey>> keyPairs, HashMap<Integer, HashMap<Integer, CBORObject>> idCreds,
            HashMap<Integer, HashMap<Integer, CBORObject>> creds, List<Integer> cipherSuites, AppProfile appProfile,
            EDP edp, HashMapCtxDB db
    ) {
        super(initiator, clientInitiated, method, connectionId, keyPairs, idCreds, creds, cipherSuites,
                appProfile, edp, db);

        fillFieldsWithDummyValues();
    }

    protected void fillFieldsWithDummyValues() {
        // own info with cipher suite 0
        setSelectedCipherSuite(Constants.EDHOC_CIPHER_SUITE_0);
        setAuthenticationCredential();
        setEphemeralKey();

        // peer dummy info
        setPeerConnectionId(new byte[]{0, 0, 0, 0});
        setPeerIdCred(CBORObject.Null);
        setPeerLongTermPublicKey(new OneKey());
        // dummy peerEphemeralPublicKey based on selectedCipherSuite 0
        OneKey peerEphemeralPublicKey = SharedSecretCalculation.buildCurve25519OneKey(null, new byte[0]);
        setPeerEphemeralPublicKey(peerEphemeralPublicKey);

        // message1 hash
        setHashMessage1(new byte[]{1});

        // plaintext2
        setPlaintext2(new byte[]{1});

        // inner key-derivation Keys
        byte[] dummyDHSecret = SharedSecretCalculation.generateSharedSecret(getEphemeralKey(), getPeerEphemeralPublicKey());
        setPRK2e(MessageProcessor.computePRK2e(dummyDHSecret, getEdhocHashAlg(getSelectedCipherSuite())));
        setPRK3e2m(MessageProcessor.computePRK3e2m(this, getPRK2e()));
        setPRK4e3m(MessageProcessor.computePRK4e3m(this));

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
}
