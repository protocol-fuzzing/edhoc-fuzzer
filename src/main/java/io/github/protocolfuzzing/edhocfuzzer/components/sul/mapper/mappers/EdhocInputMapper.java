package io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputChecker;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.InputMapper;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class EdhocInputMapper extends InputMapper<EdhocInput, EdhocOutput, EdhocProtocolMessage, EdhocExecutionContext> {
    EdhocMapperConnector edhocMapperConnector;

    public EdhocInputMapper(MapperConfig mapperConfig, EdhocOutputChecker outputChecker, EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig, outputChecker);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    protected void sendMessage(EdhocProtocolMessage message, EdhocExecutionContext context) {
        if (message == null) {
            throw new RuntimeException("Null message provided to EdhocInputMapper in sendMessage");
        }

        // enable or disable content format
        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) mapperConfig;
        int contentFormat = edhocMapperConfig.useContentFormat() ?
            message.getContentFormat(edhocMapperConfig.useOldContentFormat()) :
            MediaTypeRegistry.UNDEFINED;

        edhocMapperConnector.send(message.getPayload(), message.getPayloadType(), message.getMessageCode(), contentFormat);
    }
}
