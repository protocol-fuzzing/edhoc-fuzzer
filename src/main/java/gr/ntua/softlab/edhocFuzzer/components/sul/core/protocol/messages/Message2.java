package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import org.eclipse.californium.edhoc.Constants;

public class Message2 extends EdhocProtocolMessage {

    public Message2(MessageProcessorPersistent messageProcessorPersistent) {
        payload = messageProcessorPersistent.writeMessage2();

        if (!messageProcessorPersistent.getEdhocMapperState().getEdhocSession().isClientInitiated()) {
            // C_I is prepended
            contentFormat = Constants.APPLICATION_CID_EDHOC_CBOR_SEQ;
        }
    }
}
