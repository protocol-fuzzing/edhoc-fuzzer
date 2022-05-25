package gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;

public interface MapperBuilder {
    Mapper build(MapperConfig mapperConfig);
}
