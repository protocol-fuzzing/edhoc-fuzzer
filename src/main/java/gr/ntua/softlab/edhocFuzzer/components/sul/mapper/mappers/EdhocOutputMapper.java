package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.OutputMapper;

public class EdhocOutputMapper extends OutputMapper {
    EdhocMapperConnector edhocMapperConnector;

    public EdhocOutputMapper(MapperConfig mapperConfig, EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    public AbstractOutput receiveOutput(ExecutionContext context) {
        byte[] receivedPayload = edhocMapperConnector.receive();
        // TODO identify received message and create AbstractOutput
        return AbstractOutput.unknown();
    }
}
