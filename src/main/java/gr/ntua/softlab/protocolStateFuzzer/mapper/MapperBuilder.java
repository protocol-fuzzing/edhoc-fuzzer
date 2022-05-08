package gr.ntua.softlab.protocolStateFuzzer.mapper;

import gr.ntua.softlab.protocolStateFuzzer.mapper.config.MapperConfig;

public interface MapperBuilder {
    Mapper build(MapperConfig mapperConfig);
}
