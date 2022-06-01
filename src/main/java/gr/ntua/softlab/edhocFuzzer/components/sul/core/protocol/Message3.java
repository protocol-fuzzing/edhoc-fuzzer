package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.edhoc.EdhocSession;
import org.eclipse.californium.edhoc.MessageProcessor;

public class Message3 extends EdhocProtocolMessage {

    Message3(EdhocSession edhocSession, CBORObject[] ead) {
        cborSequence = MessageProcessor.writeMessage3(edhocSession, ead);
    }
}
