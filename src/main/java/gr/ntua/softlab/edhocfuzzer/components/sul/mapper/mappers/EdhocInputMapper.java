package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.mappers;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.protocol.ProtocolMessage;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractOutputChecker;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.context.ExecutionContext;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.InputMapper;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
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
