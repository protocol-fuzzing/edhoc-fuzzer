package gr.ntua.softlab.edhocFuzzer.components.sul.core.config;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfig;
import org.eclipse.californium.elements.config.Configuration;

import java.io.InputStream;

public class EdhocMapperConnectionConfig implements MapperConnectionConfig {
    private Configuration configuration;

    public EdhocMapperConnectionConfig(InputStream inputStream) {
        this.configuration = Configuration.createFromStream(inputStream, null);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
