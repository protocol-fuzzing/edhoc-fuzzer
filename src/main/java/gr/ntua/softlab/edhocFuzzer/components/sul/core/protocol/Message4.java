package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import org.eclipse.californium.edhoc.Constants;

public class Message4 extends EdhocProtocolMessage {

    public Message4(MessageProcessorPersistent messageProcessorPersistent) {
        payload = messageProcessorPersistent.writeMessage4();

        if (!messageProcessorPersistent.getEdhocMapperState().getEdhocSession().isClientInitiated()) {
            // C_I is prepended
            contentFormat = Constants.APPLICATION_CID_EDHOC_CBOR_SEQ;
        }
    }
}
