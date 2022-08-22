package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutputChecker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.InputMapper;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class EdhocInputMapper extends InputMapper {
    EdhocMapperConnector edhocMapperConnector;

    public EdhocInputMapper(MapperConfig mapperConfig, AbstractOutputChecker outputChecker,
                            EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig, outputChecker);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    protected void sendMessage(ProtocolMessage message, ExecutionContext context) {
        if (message == null) {
            throw new RuntimeException("Null message provided to EdhocInputMapper in sendMessage");
        }

        EdhocProtocolMessage edhocProtocolMessage = (EdhocProtocolMessage) message;

        // enable or disable content format
        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) mapperConfig;
        int contentFormat = edhocMapperConfig.useContentFormat() ? edhocProtocolMessage.getContentFormat() :
                MediaTypeRegistry.UNDEFINED;

        edhocMapperConnector.send(edhocProtocolMessage.getPayload(), edhocProtocolMessage.getPayloadType(),
                edhocProtocolMessage.getMessageCode(), contentFormat);
    }
}
