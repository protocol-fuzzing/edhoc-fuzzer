package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.authentication.AuthenticationConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfigStandard;
import com.google.common.base.Ascii;
import org.eclipse.californium.elements.util.StringUtil;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class EdhocMapperConfig extends MapperConfigStandard {
    protected String host = "";

    @ParametersDelegate
    protected AuthenticationConfig authenticationConfig = new AuthenticationConfig();

    @Parameter(names = "-protocolVersion", required = true, description = "Protocol version to be analyzed",
            converter = ProtocolVersionConverter.class)
    protected ProtocolVersion protocolVersion = null;

    @Parameter(names = "-combinedMessageVersion", description = "The version of the combined message (EDHOC + OSCORE) to use",
            converter = CombinedMessageVersionConverter.class)
    protected CombinedMessageVersion combinedMessageVersion = CombinedMessageVersion.v06;

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

    @Parameter(names = "-appMessageCodeToCoapServer", description = "The message CoAP request Code when mapper as a "
            + "CoAP client sends requests to a server implementation")
    protected String appMessageCodeToCoapServer = "GET";

    @Parameter(names = "-appMessagePayloadToCoapServer", description = "The message payload when mapper as a "
            + "CoAP client sends requests to a server implementation. Be aware that GET requests should have an "
            + "empty payload")
    protected String appMessagePayloadToCoapServer = "";

    @Parameter(names = "-appMessageCodeToCoapClient", description = "The message CoAP response Code when mapper as a "
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

    @Parameter(names = "-useOldContentFormat", description = "Use the old CoAP Content-Format (6500x) in sending messages")
    protected boolean useOldContentFormat = false;

    @Parameter(names = "-enableSessionReset", description = "Reset to default old session data, when Initiator mapper "
            + "sends a message to start a new session. Reset does not affect a Responder mapper")
    protected boolean enableSessionReset = false;

    @Parameter(names = "-disableCXCorrelation", description = "Disable correlation with connection identifiers. "
            + "In case of client mapper do not prepend CX to requests and in case of server mapper do not treat first "
            + "CBOR object in a response as CX")
    protected boolean disableCXCorrelation = false;

    @Parameter(names = "-ownConnectionId", description = "Use this id as own connection id for the EDHOC protocol. "
            + "Avoid an id that would coincide with a peer connection id found during EDHOC, in order for the OSCORE "
            + "context to be derived successfully from those two ids. If the mapper is a Responder and "
            + "`disableOwnConnectionIdGeneration` is not specified, then the `ownConnectionId` is ignored. "
            + "Available: empty byte string: [] or single-line hexadecimal byte string in the format: 0a0b0c0d0e0f.")
    protected String ownConnectionId = "36";

    @Parameter(names = "-disableOwnConnectionIdGeneration", description = "It is used only when the mapper is a Responder. "
            + "It disables the automatic generation of the mapper's connection id every time when an EDHOC Message 1 "
            + "is received from the Initiator SUT. In that case the `ownConnectionId` is used every time.")
    protected boolean disableOwnConnectionIdGeneration = false;

    @Parameter(names = "-forceOscoreSenderId", description = "Use this OSCORE sender id, instead of the peer "
            + "connection id that is found during EDHOC. Available: empty byte string: [] or single-line hexadecimal "
            + "byte string in the format: 0a0b0c0d0e0f")
    protected String forceOscoreSenderId = null;

    @Parameter(names = "-forceOscoreRecipientId", description = "Use this OSCORE recipient id, instead of the own "
            + "connection id during EDHOC. Available: empty byte string: [] or single-line hexadecimal byte string in "
            + "the format: 0a0b0c0d0e0f")
    protected String forceOscoreRecipientId = null;

    @Parameter(names = "-concretizeDir", description = "The directory to save the files regarding concretization. Note: it is better to be used during testing, instead of learning.")
    protected String concretizeDir = null;

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

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public CombinedMessageVersion getCombinedMessageVersion() {
        return combinedMessageVersion;
    }

    public String getEdhocCoapResource() {
        return edhocCoapResource;
    }

    public String getAppCoapResource() {
        return appCoapResource;
    }

    public String getAppMessageCodeToCoapServer() {
        return Ascii.toUpperCase(appMessageCodeToCoapServer);
    }

    public String getAppMessagePayloadToCoapServer() {
        return appMessagePayloadToCoapServer;
    }

    public String getAppMessageCodeToCoapClient() {
        return Ascii.toUpperCase(appMessageCodeToCoapClient);
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

    public boolean useOldContentFormat() {
        return useOldContentFormat;
    }

    public boolean useSessionReset() {
        return enableSessionReset;
    }

    public boolean useCXCorrelation() {
        return !disableCXCorrelation;
    }

    public byte[] getOwnConnectionId() {
        return parseHexString(ownConnectionId);
    }

    public boolean generateOwnConnectionId() {
        return !disableOwnConnectionIdGeneration;
    }

    public byte[] getForceOscoreSenderId() {
        return parseHexString(forceOscoreSenderId);
    }

    public byte[] getForceOscoreRecipientId() {
        return parseHexString(forceOscoreRecipientId);
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

    public String getConcretizeDir() {
        return concretizeDir;
    }

    protected String checkAndReturnHost(String host) {
        String[] hostArray = host.split(":", -1);

        if (hostArray.length != 2) {
            throw new RuntimeException("Argument provided to -connect has not the correct format: ip:port");
        }

        try {
            Integer.parseInt(hostArray[1]);
            return host;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Port number '" + hostArray[1] + "' in -connect is not an integer");
        }
    }

    protected String getCoapUri(String host, String resource) {
        try {
            String coapUri = String.format("coap://%s/%s", host, resource);
            return new URI(coapUri).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected byte[] parseHexString(String hexString) {
        if (hexString == null) {
            return null;
        }

        if (Objects.equals(hexString, "[]")) {
            return new byte[]{};
        }

        return StringUtil.hex2ByteArray(hexString);
    }

    @Override
    public void printRunDescriptionSelf(PrintWriter printWriter) {
        super.printRunDescriptionSelf(printWriter);
        printWriter.println();
        printWriter.println("EdhocMapperConfig Parameters");
        printWriter.println("Protocol Version: " + getProtocolVersion());
        printWriter.println("Combined Message Version: " + getCombinedMessageVersion());
        printWriter.println("Edhoc Role: " + edhocRole);
        printWriter.println("Edhoc Coap Resource: " + getEdhocCoapResource());
        printWriter.println("App Coap Resource: " + getAppCoapResource());
        printWriter.println("App Message Code To Coap Server: " + getAppMessageCodeToCoapServer());
        printWriter.println("App Message Payload To Coap Server: " + getAppMessagePayloadToCoapServer());
        printWriter.println("App Message Code To Coap Client: " + getAppMessageCodeToCoapClient());
        printWriter.println("App Message Payload To Coap Client: " + getAppMessagePayloadToCoapClient());
        printWriter.println("Coap Error As Edhoc Error: " + isCoapErrorAsEdhocError());
        printWriter.println("use Content Format: " + useContentFormat());
        printWriter.println("use Old Content Format: " + useOldContentFormat());
        printWriter.println("use Session Reset: " + useSessionReset());
        printWriter.println("use CX Correlation: " + useCXCorrelation());
        printWriter.println("Own Connection Id: " + this.ownConnectionId);
        printWriter.println("Generate Own Connection Id: " + generateOwnConnectionId());
        printWriter.println("Force Oscore Sender Id: " + this.forceOscoreSenderId);
        printWriter.println("Force Oscore Recipient Id: " + this.forceOscoreRecipientId);
    }

    @Override
    public void printRunDescriptionRec(PrintWriter printWriter) {
        getAuthenticationConfig().printRunDescription(printWriter);
    }
}
