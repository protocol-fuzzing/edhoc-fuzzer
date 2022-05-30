package gr.ntua.softlab.edhocFuzzer.components.sul.mapper;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocInputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocOutputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.EdhocOutputChecker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.MapperBuilder;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.MapperComposer;

public class EdhocMapperBuilder implements MapperBuilder {
    @Override
    public Mapper build(MapperConfig mapperConfig) {
        return new MapperComposer(
                new EdhocInputMapper(),
                new EdhocOutputMapper(mapperConfig),
                new EdhocOutputChecker()
        );
    }
}
