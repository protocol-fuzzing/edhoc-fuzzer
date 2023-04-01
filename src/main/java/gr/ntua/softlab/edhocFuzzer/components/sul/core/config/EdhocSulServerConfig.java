package gr.ntua.softlab.edhocFuzzer.components.sul.core.config;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulServerConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConnectionConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConnectionConfigException;
import org.eclipse.californium.elements.config.Configuration;

public class EdhocSulServerConfig extends SulServerConfig {
    public EdhocSulServerConfig(EdhocMapperConfig edhocMapperConfig) {
        super(edhocMapperConfig);
    }

    @Override
    public MapperConfig getMapperConfig() {
        ((EdhocMapperConfig) mapperConfig).initializeHost(host);
        return mapperConfig;
    }

    @Override
    public void applyDelegate(MapperConnectionConfig config) throws MapperConnectionConfigException {
        Configuration.setStandard(((EdhocMapperConnectionConfig) config).getConfiguration());
    }
}
