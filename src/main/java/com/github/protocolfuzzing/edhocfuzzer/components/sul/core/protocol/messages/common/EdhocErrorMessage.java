package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import org.eclipse.californium.edhoc.Constants;

public class EdhocErrorMessage extends EdhocProtocolMessage {

    public EdhocErrorMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = messageProcessorPersistent.writeErrorMessage(Constants.ERR_CODE_UNSPECIFIED_ERROR, "Error Message");
    }

}
