package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.edhoc.EdhocSession;
import org.eclipse.californium.edhoc.MessageProcessor;

public class Message4 extends EdhocProtocolMessage {

    Message4(EdhocSession edhocSession, CBORObject[] ead) {
        cborSequence = MessageProcessor.writeMessage4(edhocSession, ead);
    }
}
