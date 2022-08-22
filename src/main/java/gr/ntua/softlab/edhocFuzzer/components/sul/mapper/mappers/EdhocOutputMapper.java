package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.GenericErrorException;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.TimeoutException;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulServer.ClientMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs.EdhocOutputType;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.OutputMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.edhoc.Constants;

import java.util.Arrays;

public class EdhocOutputMapper extends OutputMapper {
    private static final Logger LOGGER = LogManager.getLogger(EdhocOutputMapper.class);

    EdhocMapperConnector edhocMapperConnector;

    public EdhocOutputMapper(MapperConfig mapperConfig, EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    public AbstractOutput receiveOutput(ExecutionContext context) {
        EdhocMapperState edhocMapperState = (EdhocMapperState) context.getState();
        byte[] responsePayload;

        try {
            responsePayload = edhocMapperConnector.receive();
        } catch (GenericErrorException e) {
            return socketClosed();
        } catch (TimeoutException e) {
            return timeout();
        }

        // mapper client specific possible response
        if (edhocMapperState.isCoapClient()) {
            // In case of mapper is a client
            if (((ClientMapperConnector) edhocMapperConnector).receivedEmptyCoapAck()) {
                // received empty coap ack -- possible when client is Initiator
                // and message 3 is the final edhoc message
                return new AbstractOutput(EdhocOutputType.EMPTY_COAP_ACK.name());
            }
        }

        // Check for Application Data related response
        AbstractOutput abstractOutput = appDataOutput(edhocMapperState.isCoapClient(), responsePayload);
        if (abstractOutput != null) {
            return abstractOutput;
        }

        // Check for edhoc message related response
        MessageProcessorPersistent messageProcessorPersistent = new MessageProcessorPersistent(edhocMapperState);

        int structuralMessageType = messageProcessorPersistent.messageTypeFromStructure(responsePayload);

        switch (structuralMessageType) {
            case Constants.EDHOC_ERROR_MESSAGE -> {
                boolean ok = messageProcessorPersistent.readErrorMessage(responsePayload);
                return abstractOutputAfterCheck(ok, EdhocOutputType.EDHOC_ERROR_MESSAGE.name());
            }

            case Constants.EDHOC_MESSAGE_1 -> {
                boolean ok = messageProcessorPersistent.readMessage1(responsePayload);
                return abstractOutputAfterCheck(ok, EdhocOutputType.EDHOC_MESSAGE_1.name());
            }

            case Constants.EDHOC_MESSAGE_2 -> {
                boolean ok = messageProcessorPersistent.readMessage2(responsePayload);
                return abstractOutputAfterCheck(ok, EdhocOutputType.EDHOC_MESSAGE_2.name());
            }

            case Constants.EDHOC_MESSAGE_3 -> {
                // message may be 3 or 4
                boolean ok = messageProcessorPersistent.readMessage3(responsePayload);
                if (ok) {
                    return new AbstractOutput(EdhocOutputType.EDHOC_MESSAGE_3.name());
                }

                ok = messageProcessorPersistent.readMessage4(responsePayload);
                return abstractOutputAfterCheck(ok, EdhocOutputType.EDHOC_MESSAGE_4.name());
            }

            default -> {
                return abstractOutputAfterCheck(false, null);
            }
        }
    }

    protected AbstractOutput appDataOutput(boolean isCoapClient, byte[] responsePayload) {
        String messageType = isCoapClient ? "response" : "request";

        if (edhocMapperConnector.receivedAppDataCombinedWithMsg3()) {
            // received Message3Combined, from which application data propagated and decrypted
            LOGGER.info("EDHOC_MESSAGE_3_COMBINED | APP_DATA ({}): {}", messageType,
                    Arrays.toString(responsePayload));
            return new AbstractOutput(EdhocOutputType.EDHOC_MESSAGE_3_COMBINED.name());
        }

        if (edhocMapperConnector.receivedAppData()) {
            /*
                Client Mapper:
                    sent oscore protected app data and received oscore protected
                    app data, handled by oscore layer, so responsePayload is the
                    decrypted response

                Server Mapper:
                    received oscore-protected request to application data, so
                    responsePayload is the decrypted request payload
             */
            LOGGER.info("APPLICATION_DATA ({}): {}", messageType, Arrays.toString(responsePayload));
            return new AbstractOutput(EdhocOutputType.APPLICATION_DATA.name());
        }

        // no app data related response
        return null;
    }

    protected AbstractOutput abstractOutputAfterCheck(boolean successfulCheck, String outputName) {
        if (successfulCheck) {
            return new AbstractOutput(outputName);
        }

        if (edhocMapperConnector.receivedError()) {
            return coapError();
        }

        return AbstractOutput.unknown();
    }

    protected AbstractOutput coapError() {
        if (((EdhocMapperConfig) mapperConfig).isCoapErrorAsEdhocError()) {
            return new AbstractOutput(EdhocOutputType.EDHOC_ERROR_MESSAGE.name());
        } else {
            return new AbstractOutput(EdhocOutputType.COAP_ERROR_MESSAGE.name());
        }
    }

}
