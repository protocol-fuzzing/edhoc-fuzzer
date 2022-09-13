package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class EmptyCoapMessage extends EdhocProtocolMessage {

    public EmptyCoapMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);
        payload = new byte[0];
        // no content format
        contentFormat = MediaTypeRegistry.UNDEFINED;
    }
}
