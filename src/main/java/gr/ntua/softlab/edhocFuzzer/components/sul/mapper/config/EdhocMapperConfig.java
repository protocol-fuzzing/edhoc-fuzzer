package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.authentication.AuthenticationConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import org.eclipse.californium.elements.util.StringUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class EdhocMapperConfig extends MapperConfig {
    protected String host = "";

    @ParametersDelegate
    protected AuthenticationConfig AuthenticationConfig = new AuthenticationConfig();

    @Parameter(names = "-edhocRole", required = true, description = "The Role of this peer in the edhoc protocol. " +
            "Available are: Initiator, Responder")
    protected EdhocRole edhocRole;

    @Parameter(names = "-edhocCoapResource", description = "The CoAP uri for mapper to send/receive edhoc messages. "
            + "The format is without protocol and host, e.g. for 'coap://ip:port/.well-known/edhoc', "
            + "-edhocCoapResource = .well-known/edhoc")
    protected String edhocCoapResource = ".well-known/edhoc";

    @Parameter(names = "-appCoapResource", description = "The CoAP uri for mapper to send/receive application messages. "
            + "The format is without protocol and host, e.g. for 'coap://ip:port/applicationEndpoint', "
            + "-appCoapResource = applicationEndpoint")
    protected String appCoapResource = "applicationEndpoint";

    @Parameter(names = "-appMessageCodeToCoapServer", description = "The message CoAP Code when mapper as a "
            + "CoAP client sends requests to a server implementation")
    protected String appMessageCodeToCoapServer = "GET";

    @Parameter(names = "-appMessagePayloadToCoapServer", description = "The message payload when mapper as a "
            + "CoAP client sends requests to a server implementation. Be aware that GET requests should have an "
            + "empty payload")
    protected String appMessagePayloadToCoapServer = "";

    @Parameter(names = "-appMessageCodeToCoapClient", description = "The message CoAP Code when mapper as a "
            + "CoAP server sends responses to a client implementation")
    protected String appMessageCodeToCoapClient = "CHANGED";

    @Parameter(names = "-appMessagePayloadToCoapClient", description = "The message payload when mapper as a "
            + "CoAP server sends responses to a client implementation")
    protected String appMessagePayloadToCoapClient = "Server Application Data";

    @Parameter(names = "-coapErrorAsEdhocError", description = "Use 'EDHOC_ERROR_MESSAGE' instead of "
            + "'COAP_ERROR_MESSAGE'")
    protected boolean coapErrorAsEdhocError = false;

    @Parameter(names = "-disableContentFormat", description = "Do not add CoAP Content-Format in sending messages")
    protected boolean disableContentFormat = false;

    @Parameter(names = "-disableSessionReset", description = "Do not reset old session data, when Initiator mapper " +
            "sends a new starting message. Warning: Disabling session reset may lead to inaccurate learning")
    protected boolean disableSessionReset = false;

    @Parameter(names = "-disableCXCorrelation", description = "Disable correlation with connection identifiers. " +
            "In case of client mapper do not prepend CX to requests and in case of server mapper do not treat first " +
            "CBOR object in a response as CX")
    protected boolean disableCXCorrelation = false;

    @Parameter(names = "-forceOscoreSenderId", description = "Use this oscore sender id, instead of the peer " +
            "connection id that is found during EDHOC. " +
            "Available: empty byte string: [] or single-line byte string in the format: 00 01 02 03 04 05")
    protected String forceOscoreSenderId = null;

    @Parameter(names = "-forceOscoreRecipientId", description = "Use this oscore sender id, instead of the own " +
            "connection id that is found during EDHOC. " +
            "Available: empty byte string: [] or single-line byte string in the format: 00 01 02 03 04 05")
    protected String forceOscoreRecipientId = null;

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
        return AuthenticationConfig;
    }

    public String getEdhocCoapResource() {
        return edhocCoapResource;
    }

    public String getAppCoapResource() {
        return appCoapResource;
    }

    public String getAppMessageCodeToCoapServer() {
        return appMessageCodeToCoapServer;
    }

    public String getAppMessagePayloadToCoapServer() {
        return appMessagePayloadToCoapServer;
    }

    public String getAppMessageCodeToCoapClient() {
        return appMessageCodeToCoapClient;
    }

    public String getAppMessagePayloadToCoapClient() {
        return appMessagePayloadToCoapClient;
    }

    public boolean isCoapErrorAsEdhocError() {
        return coapErrorAsEdhocError;
    }

    public boolean useContentFormat() {
        return !disableContentFormat;
    }

    public boolean useSessionReset() {
        return !disableSessionReset;
    }

    public boolean useCXCorrelation() {
        return !disableCXCorrelation;
    }

    public byte[] getForceOscoreSenderId() {
        return parseForceOscoreId(forceOscoreSenderId);
    }

    public byte[] getForceOscoreRecipientId() {
        return parseForceOscoreId(forceOscoreRecipientId);
    }

    public String getHostCoapUri() {
        return "coap://" + host;
    }

    public String getEdhocCoapUri() {
        return getCoapUri(host, edhocCoapResource);
    }

    public String getAppCoapUri() {
        return getCoapUri(host, appCoapResource);
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

    protected byte[] parseForceOscoreId(String idString) {
        if (idString == null) {
            return null;
        } else {
            return Objects.equals(idString, "[]") ? new byte[]{} : StringUtil.hex2ByteArray(idString);
        }
    }
}
