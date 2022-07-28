package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import org.eclipse.californium.edhoc.Constants;

public class Message3 extends EdhocProtocolMessage {

    public Message3(MessageProcessorPersistent messageProcessorPersistent) {
        payload = messageProcessorPersistent.writeMessage3();

        if (messageProcessorPersistent.getEdhocMapperState().getEdhocSession().isClientInitiated()) {
            // C_R is prepended
            contentFormat = Constants.APPLICATION_CID_EDHOC_CBOR_SEQ;
        }
    }
}
