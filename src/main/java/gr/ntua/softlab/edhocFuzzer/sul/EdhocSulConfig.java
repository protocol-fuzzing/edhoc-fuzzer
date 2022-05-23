package gr.ntua.softlab.edhocFuzzer.sul;

import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulConfig;
import org.eclipse.californium.elements.config.Configuration;

import java.io.InputStream;

public class EdhocSulConfig implements SulConfig {
    private Configuration sulConfig;

    public EdhocSulConfig(InputStream inputStream) {
        this.sulConfig = Configuration.createFromStream(inputStream, null);
    }

    public Configuration getSulConfig() {
        return sulConfig;
    }
}
