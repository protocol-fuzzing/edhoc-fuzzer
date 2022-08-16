package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.PayloadType;
import org.eclipse.californium.core.coap.CoAP;

public class ApplicationDataMessage extends EdhocProtocolMessage {

    public ApplicationDataMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);

        if (messageProcessorPersistent.getEdhocMapperState().isCoapClient()) {
            // In case of CoAP client send GET request

            // empty payload
            payload = new byte[0];
            // GET request
            messageCode = CoAP.Code.GET.value;
        } else {
            // In case of CoAP server send something back

            // payload to be sent
            payload = "Server Application Data".getBytes();
            // changed response code
            messageCode = CoAP.ResponseCode.CHANGED.value;
        }

        // -1 means null content format
        contentFormat = -1;
        // oscore-protected application message
        payloadType = PayloadType.APPLICATION_DATA;
    }
}
