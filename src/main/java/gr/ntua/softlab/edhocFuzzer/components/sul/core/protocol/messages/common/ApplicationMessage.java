package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocMapperState;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public abstract class ApplicationMessage extends EdhocProtocolMessage {

    public ApplicationMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);

        EdhocMapperState edhocMapperState = messageProcessorPersistent.getEdhocMapperState();
        EdhocMapperConfig edhocMapperConfig = edhocMapperState.getEdhocMapperConfig();

        if (edhocMapperState.isCoapClient()) {
            // In case of CoAP client send requests to CoAP server

            payload = edhocMapperConfig.getAppMessagePayloadToCoapServer().getBytes();
            messageCode = CoAP.Code.valueOf(edhocMapperConfig.getAppMessageCodeToCoapServer()).value;
            contentFormat = MediaTypeRegistry.UNDEFINED;
        } else {
            // In case of CoAP server send responses to CoAP client

            payload = edhocMapperConfig.getAppMessagePayloadToCoapClient().getBytes();
            messageCode = CoAP.ResponseCode.valueOf(edhocMapperConfig.getAppMessageCodeToCoapClient()).value;
            contentFormat = MediaTypeRegistry.TEXT_PLAIN;
        }
    }
}
