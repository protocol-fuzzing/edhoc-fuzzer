package gr.ntua.softlab.edhocFuzzer.components.sul.core.config;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulClientConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.MapperToSulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.MapperToSulConfigException;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;

public class EdhocSulClientConfig extends SulClientConfig {
    public EdhocSulClientConfig(MapperConfig mapperConfig) {
        super(mapperConfig);
    }

    @Override
    public void applyDelegate(MapperToSulConfig config) throws MapperToSulConfigException {
    }
}
