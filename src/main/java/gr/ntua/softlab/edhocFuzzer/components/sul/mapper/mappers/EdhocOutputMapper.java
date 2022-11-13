package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.*;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs.MessageOutputType;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.OutputMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        } catch (UnsupportedMessageException e) {
            // special output to demonstrate that the input message the learner requested
            // was unable to be sent and deemed unsupported
            return new AbstractOutput(MessageOutputType.UNSUPPORTED_MESSAGE.name());
        } catch (UnsuccessfulMessageException e) {
            // special output to demonstrate that the received message evoked an error
            // in a middle layer and did not reach the upper resource in the case of
            // server mapper
            return new AbstractOutput(MessageOutputType.UNSUCCESSFUL_MESSAGE.name());
        }

        AbstractOutput abstractOutput;

        // Check for application related message
        // including message 3 combined with oscore
        abstractOutput = appOutput(edhocMapperState, responsePayload);

        if (abstractOutput != null) {
            return abstractOutput;
        }

        // Check for edhoc message
        abstractOutput = edhocOutput(edhocMapperState, responsePayload);

        if (abstractOutput != null) {
            return abstractOutput;
        }

        // Check for coap message
        abstractOutput = coapOutput(edhocMapperState, responsePayload);

        if (abstractOutput != null) {
            return abstractOutput;
        }

        return AbstractOutput.unknown();
    }

    protected AbstractOutput appOutput(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        String messageType = edhocMapperState.isCoapClient() ? "response" : "request";

        if (edhocMapperConnector.receivedMsg3WithOscoreApp()) {
            // received Message3_OSCORE_APP, from which application data propagated and decrypted
            LOGGER.info("EDHOC_MESSAGE_3_OSCORE_APP | OSCORE_APP_MESSAGE ({}): {} ~ {}",
                    messageType, Arrays.toString(responsePayload), new String(responsePayload));

            return new AbstractOutput(MessageOutputType.EDHOC_MESSAGE_3_OSCORE_APP.name());
        }

        if (edhocMapperConnector.receivedOscoreAppMessage()) {
            /*
                Client Mapper:
                    sent oscore protected app data and received oscore protected
                    app data, handled by oscore layer, so responsePayload is the
                    decrypted response

                Server Mapper:
                    received oscore-protected request to application data, so
                    responsePayload is the decrypted request payload
             */
            LOGGER.info("OSCORE_APP_MESSAGE ({}): {} ~ {}",
                    messageType, Arrays.toString(responsePayload), new String(responsePayload));

            return new AbstractOutput(MessageOutputType.OSCORE_APP_MESSAGE.name());
        }

        return null;
    }

    protected AbstractOutput edhocOutput(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        MessageProcessorPersistent messageProcessorPersistent = new MessageProcessorPersistent(edhocMapperState);
        boolean ok;

        switch(messageProcessorPersistent.messageTypeFromStructure(responsePayload)) {
            case EDHOC_ERROR_MESSAGE -> {
                ok = messageProcessorPersistent.readErrorMessage(responsePayload);
                return abstractOutputAfterCheck(ok, MessageOutputType.EDHOC_ERROR_MESSAGE.name());
            }

            case EDHOC_MESSAGE_1 -> {
                ok = messageProcessorPersistent.readMessage1(responsePayload);
                return abstractOutputAfterCheck(ok, MessageOutputType.EDHOC_MESSAGE_1.name());
            }

            case EDHOC_MESSAGE_2 -> {
                ok = messageProcessorPersistent.readMessage2(responsePayload);
                return abstractOutputAfterCheck(ok, MessageOutputType.EDHOC_MESSAGE_2.name());
            }

            case EDHOC_MESSAGE_3_OR_EDHOC_MESSAGE_4 -> {
                // message may be 3 or 4
                ok = messageProcessorPersistent.readMessage3(responsePayload);
                if (ok) {
                    return new AbstractOutput(MessageOutputType.EDHOC_MESSAGE_3.name());
                }

                ok = messageProcessorPersistent.readMessage4(responsePayload);
                return abstractOutputAfterCheck(ok, MessageOutputType.EDHOC_MESSAGE_4.name());
            }

            default -> {
                return null;
            }
        }
    }

    protected AbstractOutput coapOutput(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        String messageType = edhocMapperState.isCoapClient() ? "response" : "request";

        // Check for coap error message
        if (edhocMapperConnector.receivedCoapErrorMessage()) {
            return coapError();
        }

        // Check for coap empty message
        if (edhocMapperConnector.receivedCoapEmptyMessage()) {
            /*
                Client Mapper:
                    received empty coap ack, possible when client mapper is
                    Initiator and message 3 is the final edhoc message

                Server Mapper:
                    received empty coap request for some reason
             */
            return new AbstractOutput(MessageOutputType.COAP_EMPTY_MESSAGE.name());
        }

        // Check for unprotected coap message
        // Must be checked after checking for empty message, because empty message can be unprotected
        // Application message is any non-error coap message, no distinction based on payload
        if (edhocMapperConnector.receivedCoapAppMessage()) {
            LOGGER.info("COAP_APP_MESSAGE ({}): {} ~ {}",
                messageType, Arrays.toString(responsePayload), new String(responsePayload));
            return new AbstractOutput(MessageOutputType.COAP_APP_MESSAGE.name());
        }

        // if payload was not empty then a coap message is received
        // because no other transport protocol than coap is supported yet
        return abstractOutputAfterCheck(responsePayload != null, MessageOutputType.COAP_MESSAGE.name());
    }

    protected AbstractOutput abstractOutputAfterCheck(boolean successfulCheck, String outputName) {
        return successfulCheck ? new AbstractOutput(outputName) : null;
    }

    protected AbstractOutput coapError() {
        if (((EdhocMapperConfig) mapperConfig).isCoapErrorAsEdhocError()) {
            return new AbstractOutput(MessageOutputType.EDHOC_ERROR_MESSAGE.name());
        } else {
            return new AbstractOutput(MessageOutputType.COAP_ERROR_MESSAGE.name());
        }
    }

}
