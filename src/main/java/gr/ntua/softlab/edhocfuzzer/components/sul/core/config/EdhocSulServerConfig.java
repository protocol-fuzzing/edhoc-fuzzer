package gr.ntua.softlab.edhocfuzzer.components.sul.core.config;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulServerConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConnectionConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import org.eclipse.californium.elements.config.Configuration;

public class EdhocSulServerConfig extends SulServerConfig {
    public EdhocSulServerConfig(EdhocMapperConfig edhocMapperConfig) {
        super(edhocMapperConfig, null);
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
