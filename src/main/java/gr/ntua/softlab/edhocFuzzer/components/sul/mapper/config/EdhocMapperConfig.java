package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.AuthenticationConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class EdhocMapperConfig extends MapperConfig {
    protected String host = "";

    @ParametersDelegate
    protected AuthenticationConfig authenticationConfig = new AuthenticationConfig();

    @Parameter(names = "-appProfileMode", description = "The mode, under which, application profile will be set. "
            +"Available modes are: 1 (message_3 with no application message), 2 (message_3 and application message), "
            + "3 (message_3 combined with application message), 4 (message_4 with no application message), "
            + "5 (message_4 with application message). Learning alphabet should be corresponding with it.")
    protected Integer appProfileMode = 1;

    @Parameter(names = "-edhocCoapResource", description = "The CoAP uri for mapper to send/receive edhoc messages. "
            + "The format is without protocol and host, e.g. for 'coap://ip:port/.well-known/edhoc', "
            + "-edhocCoapResource = .well-known/edhoc")
    protected String edhocCoapResource = ".well-known/edhoc";

    @Parameter(names = "-appGetCoapResource", description = "The CoAP uri for mapper to send/receive oscore-protected "
            + "application GET messages. The format is without protocol and host, e.g. for "
            + "'coap://ip:port/applicationGET', -appGetCoapResource = applicationGET")
    protected String appGetCoapResource = "applicationGET";

    @Parameter(names = "-coapErrorAsEdhocError", description = "Uses 'EDHOC_ERROR_MESSAGE' instead of "
            + "'COAP_ERROR_MESSAGE'")
    protected boolean coapErrorAsEdhocError = false;

    @Parameter(names = "-disableContentFormat", description = "Do not add CoAP Content-Format in sending messages")
    protected boolean disableContentFormat = false;

    public void initializeHost(String host) {
        if (Objects.equals(this.host, "")) {
            this.host = checkAndReturnHost(host);
        }
    }

    public AuthenticationConfig getAuthenticationConfig() {
        return authenticationConfig;
    }

    public Integer getAppProfileMode() {
        return appProfileMode;
    }

    public String getEdhocCoapResource() {
        return edhocCoapResource;
    }

    public String getAppGetCoapResource() {
        return appGetCoapResource;
    }

    public boolean useContentFormat() {
        return !disableContentFormat;
    }

    public boolean isCoapErrorAsEdhocError() {
        return coapErrorAsEdhocError;
    }

    public String getEdhocCoapUri() {
        return getCoapUri(host, edhocCoapResource);
    }

    public String getAppGetCoapUri() {
        return getCoapUri(host, appGetCoapResource);
    }

    protected String checkAndReturnHost(String host) {
        String[] hostArray = host.split(":");

        if (hostArray.length != 2) {
            throw new RuntimeException("Argument provided to -connect has not the correct format: ip:port");
        }

        try{
            Integer.parseInt(hostArray[1]);
            return host;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Port number '" + hostArray[1] + "' in -connect is not an integer");
        }
    }

    protected String getCoapUri(String host, String resource) {
        try{
            String coapUri = String.format("coap://%s/%s", host, resource);
            return (new URI(coapUri)).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
