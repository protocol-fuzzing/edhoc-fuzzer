package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.config;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConnectionConfig;
import org.eclipse.californium.elements.config.Configuration;

import java.io.InputStream;

public class EdhocMapperConnectionConfig implements MapperConnectionConfig {
    private Configuration configuration;

    public EdhocMapperConnectionConfig(InputStream inputStream) {
        this.configuration = inputStream == null ? Configuration.getStandard() : Configuration.createFromStream(inputStream, null);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
