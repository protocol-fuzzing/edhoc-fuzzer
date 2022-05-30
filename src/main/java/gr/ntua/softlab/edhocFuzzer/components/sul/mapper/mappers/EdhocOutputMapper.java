package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.OutputMapper;

public class EdhocOutputMapper extends OutputMapper {
    public EdhocOutputMapper(MapperConfig mapperConfig) {
        super(mapperConfig);
    }

    @Override
    public AbstractOutput receiveOutput(ExecutionContext context) {
        return null;
    }
}
