package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.edhoc.EdhocSession;
import org.eclipse.californium.edhoc.MessageProcessor;

public class ErrorMessage extends EdhocProtocolMessage {

    public ErrorMessage(int errorCode, int replyTo, boolean isErrorReq, byte[] cX, String errMsg, CBORObject suitesR) {
        cborSequence = MessageProcessor.writeErrorMessage(errorCode, replyTo, isErrorReq, cX, errMsg, suitesR);
    }
}
