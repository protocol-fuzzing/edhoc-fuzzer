package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import org.eclipse.californium.edhoc.Constants;

public class ErrorMessage extends EdhocProtocolMessage {

    public ErrorMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeErrorMessage(Constants.ERR_CODE_UNSPECIFIED_ERROR, "Error Message");
    }

}
