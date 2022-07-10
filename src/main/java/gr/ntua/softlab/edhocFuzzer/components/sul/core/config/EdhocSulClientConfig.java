package gr.ntua.softlab.edhocFuzzer.components.sul.core.config;

import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.AuthenticationConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulClientConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfigException;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import org.eclipse.californium.elements.config.Configuration;

public class EdhocSulClientConfig extends SulClientConfig {
    @ParametersDelegate
    protected AuthenticationConfig authenticationConfig;

    public EdhocSulClientConfig(MapperConfig mapperConfig) {
        super(mapperConfig);
        this.authenticationConfig = new AuthenticationConfig();
    }

    @Override
    public void applyDelegate(MapperConnectionConfig config) throws MapperConnectionConfigException {
        Configuration.setStandard(((EdhocMapperConnectionConfig) config).getConfiguration());
    }

    public AuthenticationConfig getAuthenticationFileConfig() {
        return authenticationConfig;
    }
}
