package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import org.eclipse.californium.edhoc.Constants;

public class ErrorMessage extends EdhocProtocolMessage {
    protected MessageProcessorPersistent messageProcessorPersistent;
    public ErrorMessage(MessageProcessorPersistent messageProcessorPersistent) {
        this.messageProcessorPersistent = messageProcessorPersistent;
    }

    public ErrorMessage createAsInitiatorWithoutSuites() {
        payload = messageProcessorPersistent.writeErrorMessage(
                Constants.ERR_CODE_UNSPECIFIED_ERROR, Constants.EDHOC_MESSAGE_1, true,
                null, "Error Message", null);
        return this;
    }
}
