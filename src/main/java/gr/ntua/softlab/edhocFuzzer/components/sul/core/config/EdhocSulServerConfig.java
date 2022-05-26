package gr.ntua.softlab.edhocFuzzer.components.sul.core.config;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfigException;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulServerConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;

public class EdhocSulServerConfig extends SulServerConfig {
    public EdhocSulServerConfig(MapperConfig mapperConfig) {
        super(mapperConfig);
    }

    @Override
    public void applyDelegate(MapperConnectionConfig config) throws MapperConnectionConfigException {
    }
}
