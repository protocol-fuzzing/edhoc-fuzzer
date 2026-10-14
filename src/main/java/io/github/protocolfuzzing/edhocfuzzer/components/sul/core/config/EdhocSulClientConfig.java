package io.github.protocolfuzzing.edhocfuzzer.components.sul.core.config;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SULAdapterConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SULClientConfigStandard;
import io.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import org.eclipse.californium.elements.config.Configuration;

public class EdhocSulClientConfig extends SULClientConfigStandard {
    public EdhocSulClientConfig(EdhocMapperConfig edhocMapperConfig) {
        super(edhocMapperConfig, new SULAdapterConfig(){});
    }

    @Override
    public MapperConfig getMapperConfig() {
        ((EdhocMapperConfig) mapperConfig).initializeHost("localhost:" + this.port);
        return mapperConfig;
    }

    @Override
    public <MC> void applyDelegate(MC config) {
        if (config instanceof EdhocMapperConnectionConfig) {
            Configuration.setStandard(EdhocMapperConnectionConfig.class.cast(config).getConfiguration());
        }
    }
}
