package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import org.eclipse.californium.edhoc.Constants;

public class Message1 extends EdhocProtocolMessage {

    public Message1(MessageProcessorPersistent messageProcessorPersistent) {
        payload = messageProcessorPersistent.writeMessage1();

        if (messageProcessorPersistent.getEdhocMapperState().getEdhocSession().isClientInitiated()) {
            // CBOR simple value 'true' is prepended
            contentFormat = Constants.APPLICATION_CID_EDHOC_CBOR_SEQ;
        }
    }
}
