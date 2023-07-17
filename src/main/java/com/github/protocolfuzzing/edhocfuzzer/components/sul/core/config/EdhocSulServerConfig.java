package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulAdapterConfigEmpty;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulServerConfigStandard;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConnectionConfig;
import org.eclipse.californium.elements.config.Configuration;

public class EdhocSulServerConfig extends SulServerConfigStandard {
    public EdhocSulServerConfig(EdhocMapperConfig edhocMapperConfig) {
        super(edhocMapperConfig, new SulAdapterConfigEmpty());
    }

    @Override
    public MapperConfig getMapperConfig() {
        ((EdhocMapperConfig) mapperConfig).initializeHost(host);
        return mapperConfig;
    }

    @Override
    public void applyDelegate(MapperConnectionConfig config) {
        Configuration.setStandard(((EdhocMapperConnectionConfig) config).getConfiguration());
    }
}
