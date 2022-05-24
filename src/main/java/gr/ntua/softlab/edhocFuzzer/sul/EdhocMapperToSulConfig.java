package gr.ntua.softlab.edhocFuzzer.sul;

import gr.ntua.softlab.protocolStateFuzzer.sul.config.MapperToSulConfig;
import org.eclipse.californium.elements.config.Configuration;

import java.io.InputStream;

public class EdhocMapperToSulConfig implements MapperToSulConfig {
    private Configuration configuration;

    public EdhocMapperToSulConfig(InputStream inputStream) {
        this.configuration = Configuration.createFromStream(inputStream, null);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
