package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.edhoc.EdhocSession;
import org.eclipse.californium.edhoc.MessageProcessor;

public class Message1 extends EdhocProtocolMessage {

    public Message1(EdhocSession edhocSession, CBORObject[] ead) {
        cborSequence = MessageProcessor.writeMessage1(edhocSession, ead);
    }
}
