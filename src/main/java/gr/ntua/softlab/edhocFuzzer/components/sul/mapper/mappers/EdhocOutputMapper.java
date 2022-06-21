package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.MapperConnector;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.OutputMapper;

public class EdhocOutputMapper extends OutputMapper {
    MapperConnector mapperConnector;

    public EdhocOutputMapper(MapperConfig mapperConfig, MapperConnector mapperConnector) {
        super(mapperConfig);
        this.mapperConnector = mapperConnector;
    }

    @Override
    public AbstractOutput receiveOutput(ExecutionContext context) {
        byte[] receivedPayload = mapperConnector.receive();
        // TODO
        return null;
    }
}
