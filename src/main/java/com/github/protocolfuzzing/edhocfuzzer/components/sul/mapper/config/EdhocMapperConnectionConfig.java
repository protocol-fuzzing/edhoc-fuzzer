package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConnectionConfig;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;

import java.io.InputStream;

public class EdhocMapperConnectionConfig implements MapperConnectionConfig {
    private static boolean registered = false;
    private static void register() {
        if (!registered) {
            CoapConfig.register();
            registered = true;
        }
    }

    private Configuration configuration;

    public EdhocMapperConnectionConfig(InputStream inputStream) {
        register();
        this.configuration = inputStream == null ? Configuration.getStandard() : Configuration.createStandardFromStream(inputStream);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
