package gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.common;

import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.nio.charset.StandardCharsets;

public abstract class AppMessage extends EdhocProtocolMessage {

    public AppMessage(MessageProcessorPersistent messageProcessorPersistent) {
        super(messageProcessorPersistent);

        EdhocMapperState edhocMapperState = messageProcessorPersistent.getEdhocMapperState();
        EdhocMapperConfig edhocMapperConfig = edhocMapperState.getEdhocMapperConfig();

        if (edhocMapperState.isCoapClient()) {
            // In case of CoAP client send requests to CoAP server

            payload = edhocMapperConfig.getAppMessagePayloadToCoapServer().getBytes(StandardCharsets.UTF_8);
            messageCode = CoAP.Code.valueOf(edhocMapperConfig.getAppMessageCodeToCoapServer()).value;
            contentFormat = MediaTypeRegistry.UNDEFINED;
        } else {
            // In case of CoAP server send responses to CoAP client

            payload = edhocMapperConfig.getAppMessagePayloadToCoapClient().getBytes(StandardCharsets.UTF_8);
            messageCode = CoAP.ResponseCode.valueOf(edhocMapperConfig.getAppMessageCodeToCoapClient()).value;
            contentFormat = MediaTypeRegistry.TEXT_PLAIN;
        }
    }
}
