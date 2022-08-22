package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class ApplicationDataMessage extends EdhocProtocolMessage {

    public ApplicationDataMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);

        if (messageProcessorPersistent.getEdhocMapperState().isCoapClient()) {
            // In case of CoAP client send GET request

            // empty payload
            payload = new byte[0];
            // GET request
            messageCode = CoAP.Code.GET.value;
            // no content format
            contentFormat = MediaTypeRegistry.UNDEFINED;
        } else {
            // In case of CoAP server send something back
            // Note that in case of a non-oscore protected request
            // the response won't be oscore-protected

            // payload to be sent (oscore-protected or not)
            payload = "Server Application Data".getBytes(CoAP.UTF8_CHARSET);
            // changed response code
            messageCode = CoAP.ResponseCode.CHANGED.value;
            // text plain content format
            contentFormat = MediaTypeRegistry.TEXT_PLAIN;
        }

        // oscore-protected application message
        payloadType = PayloadType.APPLICATION_DATA;
    }
}
