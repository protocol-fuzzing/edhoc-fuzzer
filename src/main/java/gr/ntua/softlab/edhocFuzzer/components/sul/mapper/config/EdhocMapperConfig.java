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

    @Parameter(names = "-edhocRole", required = true, description = "The Role of this peer in the edhoc protocol. " +
            "Available are: Initiator, Responder")
    protected EdhocRole edhocRole;

    @Parameter(names = "-appProfileMode", description = "The mode, under which, application profile will be set. "
            + "Available modes are: "
            + "1 - [m3_no_app] (msg_1, msg_2, msg_3, error_msg), "
            + "2 - [m3_app] (msg_1, msg_2, msg_3, app, error_msg), "
            + "3 - [m3_combined_app] (msg_1, msg_2, msg_3, app, msg_3_app, error_msg), "
            + "4 - [m4_no_app] (msg_1, msg_2, msg_3, msg_4, error_msg), "
            + "5 - [m4_app] (msg_1, msg_2, msg_3, msg_4, app, error_msg), "
            + "6 - [all] (msg_1, msg_2, msg_3, msg_4, app, msg_3_app, error_msg). "
            + "If learning alphabet contains a message not according to this mode, the correct use of this message "
            + "is not guaranteed.")
    protected Integer appProfileMode = 6;

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

    public boolean isInitiator() {
        return edhocRole == EdhocRole.Initiator;
    }

    public boolean isResponder() {
        return edhocRole == EdhocRole.Responder;
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

    public String getHostCoapUri() {
        return "coap://" + host;
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
