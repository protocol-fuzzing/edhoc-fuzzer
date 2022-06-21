package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.edhoc.EdhocSession;
import org.eclipse.californium.edhoc.MessageProcessor;

public class Message2 extends EdhocProtocolMessage {

    public Message2(EdhocSession edhocSession, CBORObject[] ead) {
        cborSequence = MessageProcessor.writeMessage2(edhocSession, ead);
    }
}
