package gr.ntua.softlab.edhocFuzzer.components.sul.core.config;

import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.authentication.AuthenticationConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfigException;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulServerConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import org.eclipse.californium.elements.config.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

public class EdhocSulServerConfig extends SulServerConfig {

    @ParametersDelegate
    protected AuthenticationConfig authenticationConfig;

    public EdhocSulServerConfig(MapperConfig mapperConfig) {
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

    public String getCoapHostURI() {
        String host = getHost();
        String[] hostArray = host.split(":");

        if (hostArray.length != 2) {
            throw new RuntimeException("Argument provided to -connect has not the correct format: ip:port");
        }

        try{
            Integer.parseInt(hostArray[1]);
            String edhocURI = "coap://" + host + "/.well-known/edhoc";
            return (new URI(edhocURI)).toString();
        } catch (NumberFormatException e) {
            throw new RuntimeException("Port number '" + hostArray[1] + "' in -connect is not an integer");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
