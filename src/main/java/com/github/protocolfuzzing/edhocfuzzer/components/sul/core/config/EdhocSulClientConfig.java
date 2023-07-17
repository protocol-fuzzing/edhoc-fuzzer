package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulAdapterConfigEmpty;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulClientConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConnectionConfig;
import org.eclipse.californium.elements.config.Configuration;

public class EdhocSulClientConfig extends SulClientConfigStandard {
    public EdhocSulClientConfig(EdhocMapperConfig edhocMapperConfig) {
        super(edhocMapperConfig, new SulAdapterConfigEmpty());
    }

    @Override
    public MapperConfig getMapperConfig() {
        ((EdhocMapperConfig) mapperConfig).initializeHost("localhost:" + this.port);
        return mapperConfig;
    }

    @Override
    public void applyDelegate(MapperConnectionConfig config) {
        Configuration.setStandard(((EdhocMapperConnectionConfig) config).getConfiguration());
    }
}
