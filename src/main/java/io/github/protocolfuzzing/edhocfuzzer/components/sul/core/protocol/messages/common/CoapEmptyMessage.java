package io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class CoapEmptyMessage extends EdhocProtocolMessage {

    public CoapEmptyMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = new byte[0];
        // no content format
        contentFormat = MediaTypeRegistry.UNDEFINED;
    }
}
