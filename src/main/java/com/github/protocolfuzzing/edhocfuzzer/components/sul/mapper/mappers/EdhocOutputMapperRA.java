package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocUtil;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.GenericErrorException;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.TimeoutException;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.UnsuccessfulMessageException;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.UnsupportedMessageException;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputBuilderRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.MessageOutputTypeRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.EnumAlphabet;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.OutputMapperRA;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class EdhocOutputMapperRA
        extends OutputMapperRA<PSymbolInstance, EdhocProtocolMessage, EdhocExecutionContextRA> {
    private static final Logger LOGGER = LogManager.getLogger();

    EdhocMapperConnector edhocMapperConnector;

    EnumAlphabet alphabet;

    // FIXME: Suppressing the warning that outputBuilder shadows the same name in
    // parent.
    // If the enum approach is taken and is standardized this will be solved.
    // Hack to allow building unsupportedMessage/unknownMessage.
    @SuppressWarnings("HidingField")
    EdhocOutputBuilderRA outputBuilder;

    public EdhocOutputMapperRA(MapperConfig mapperConfig, EdhocOutputBuilderRA edhocOutputBuilder,
            EdhocMapperConnector edhocMapperConnector, EnumAlphabet alphabet) {
        super(mapperConfig, edhocOutputBuilder);
        this.edhocMapperConnector = edhocMapperConnector;
        this.outputBuilder = edhocOutputBuilder;
        this.alphabet = alphabet;
    }

    @Override
    public PSymbolInstance receiveOutput(EdhocExecutionContextRA context) {
        EdhocMapperState edhocMapperState = context.getState();
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
            return outputBuilder.buildUnsupportedMessage();
        } catch (UnsuccessfulMessageException e) {
            // special output to demonstrate that the received message evoked an error
            // in a middle layer and did not reach the upper resource in the case of
            // server mapper
            return outputBuilder.buildUnsuccessfulMessage();
        }

        PSymbolInstance abstractOutput;

        // Check for application related message
        // including message 3 combined with oscore
        abstractOutput = appOutput(edhocMapperState, responsePayload);

        if (abstractOutput != null) {
            return abstractOutput;
        }

        // Check for edhoc message
        abstractOutput = edhocOutputRA(edhocMapperState, responsePayload);

        if (abstractOutput != null) {
            return abstractOutput;
        }

        // Check for coap message
        abstractOutput = coapOutput(edhocMapperState, responsePayload);

        if (abstractOutput != null) {
            return abstractOutput;
        }

        return outputBuilder.buildUnknown();
    }

    protected PSymbolInstance appOutput(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        String messageType = edhocMapperState.isCoapClient() ? "response" : "request";

        if (edhocMapperConnector.receivedMsg3WithOscoreApp()) {
            // received Message3_OSCORE_APP, from which application data propagated and
            // decrypted
            LOGGER.info("EDHOC_MESSAGE_3_OSCORE_APP | OSCORE_APP_MESSAGE ({}): {} ~ {}",
                    messageType, EdhocUtil.byteArrayToString(responsePayload),
                    new String(responsePayload, StandardCharsets.UTF_8));

            Integer parameter = EdhocUtil
                    .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
            OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_3_OSCORE_APP_OUTPUT);

            LOGGER.info("Reading as EDHOC Message 3 Oscore App, DataValue: " + parameter);
            DataValue<Integer> value = new DataValue<Integer>(base.getPtypes()[0], parameter);

            return instanceFromOutputSymbol(base, value);
        }

        if (edhocMapperConnector.receivedOscoreAppMessage()) {
            /*
             * Client Mapper:
             * sent oscore protected app data and received oscore protected
             * app data, handled by oscore layer, so responsePayload is the
             * decrypted response
             *
             * Server Mapper:
             * received oscore-protected request to application data, so
             * responsePayload is the decrypted request payload
             */
            LOGGER.info("OSCORE_APP_MESSAGE ({}): {} ~ {}",
                    messageType, EdhocUtil.byteArrayToString(responsePayload),
                    new String(responsePayload, StandardCharsets.UTF_8));

            OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.OSCORE_APP_MESSAGE_OUTPUT);
            return instanceFromOutputSymbol(base);

        }

        return null;
    }

    protected PSymbolInstance edhocOutputRA(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        MessageProcessorPersistent messageProcessorPersistent = new MessageProcessorPersistent(edhocMapperState);
        boolean ok;

        switch (messageProcessorPersistent.messageTypeFromStructure(responsePayload)) {
            case EDHOC_ERROR_MESSAGE -> {
                ok = messageProcessorPersistent.readErrorMessage(responsePayload);
                OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_ERROR_MESSAGE_OUTPUT);
                return instanceAfterCheck(ok, base);
            }

            case EDHOC_MESSAGE_1 -> {
                ok = messageProcessorPersistent.readMessage1(responsePayload);

                Integer parameter = EdhocUtil
                        .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_1_OUTPUT);
                LOGGER.info("Reading as Message 1, DataValue: " + parameter);
                return instanceAfterCheck(ok, base);
            }

            case EDHOC_MESSAGE_2 -> {
                ok = messageProcessorPersistent.readMessage2(responsePayload);

                Integer parameter = EdhocUtil
                OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_2_OUTPUT);
                LOGGER.info("Reading as Message 2, DataValue: " + parameter);
                DataValue<Integer> value = new DataValue<Integer>(base.getPtypes()[0], parameter);
                return instanceAfterCheck(ok, base, value);
            }

            case EDHOC_MESSAGE_3_OR_4 -> {
                // message may be 3 or 4
                LOGGER.info("Reading as EDHOC Message 3 or 4");
                ok = messageProcessorPersistent.readMessage3(responsePayload);

                if (ok) {
                    OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_3_OUTPUT);
                    return instanceFromOutputSymbol(base);
                }

                ok = messageProcessorPersistent.readMessage4(responsePayload);
                OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_4_OUTPUT);
                return instanceAfterCheck(ok, base);
            }

            case EDHOC_MESSAGE_2_OR_3_OR_4 -> {
                // message may be 2 or 3 or 4
                LOGGER.info("Reading as EDHOC Message 2 or 3 or 4");
                ok = messageProcessorPersistent.readMessage2(responsePayload);
                if (ok) {
                    Integer parameter = EdhocUtil
                    OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_2_OUTPUT);
                    LOGGER.info("Reading as Message 2, DataValue: " + parameter);
                    DataValue<Integer> value = new DataValue<Integer>(base.getPtypes()[0], parameter);
                    return instanceFromOutputSymbol(base, value);
                }

                ok = messageProcessorPersistent.readMessage3(responsePayload);
                if (ok) {
                    OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_3_OUTPUT);
                    return instanceFromOutputSymbol(base);
                }

                ok = messageProcessorPersistent.readMessage4(responsePayload);
                OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_MESSAGE_4_OUTPUT);
                return instanceAfterCheck(ok, base);
            }

            default -> {
                return null;
            }
        }
    }

    protected PSymbolInstance coapOutput(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        String messageType = edhocMapperState.isCoapClient() ? "response" : "request";

        // Check for coap error message
        if (edhocMapperConnector.receivedCoapErrorMessage()) {
            LOGGER.info("COAP_ERROR_MESSAGE ({}): {} ~ {}",
                    messageType, EdhocUtil.byteArrayToString(responsePayload),
                    new String(responsePayload, StandardCharsets.UTF_8));
            return coapError();
        }

        // Check for coap empty message
        if (edhocMapperConnector.receivedCoapEmptyMessage()) {
            /*
             * Client Mapper:
             * received empty coap ack, possible when client mapper is
             * Initiator and message 3 is the final edhoc message
             *
             * Server Mapper:
             * received empty coap request for some reason
             */
            OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.COAP_EMPTY_MESSAGE_OUTPUT);
            return instanceFromOutputSymbol(base);
        }

        // Check for unprotected coap message
        // Must be checked after checking for empty message, because empty message can
        // be unprotected
        // Application message is any non-error coap message, no distinction based on
        // payload
        if (edhocMapperConnector.receivedCoapAppMessage()) {
            LOGGER.info("COAP_APP_MESSAGE ({}): {} ~ {}",
                    messageType, EdhocUtil.byteArrayToString(responsePayload),
                    new String(responsePayload, StandardCharsets.UTF_8));
            OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.COAP_APP_MESSAGE_OUTPUT);
            return instanceFromOutputSymbol(base);
        }

        // if payload was not empty then a coap message is received
        // because no other transport protocol than coap is supported yet
        OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.COAP_MESSAGE_OUTPUT);
        if (responsePayload != null) {
            LOGGER.info("COAP_MESSAGE ({}): {} ~ {}",
                    messageType, EdhocUtil.byteArrayToString(responsePayload),
                    new String(responsePayload, StandardCharsets.UTF_8));
        } else {
            LOGGER.info("COAP_MESSAGE payload is null.");
        }
        return instanceAfterCheck(responsePayload != null, base);
    }

    protected PSymbolInstance coapError() {
        if (((EdhocMapperConfig) mapperConfig).isCoapErrorAsEdhocError()) {
            OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.EDHOC_ERROR_MESSAGE_OUTPUT);
            return instanceFromOutputSymbol(base);
        } else {
            OutputSymbol base = newBaseSymbol(MessageOutputTypeRA.COAP_ERROR_MESSAGE_OUTPUT);
            return instanceFromOutputSymbol(base);
        }
    }

    protected OutputSymbol newBaseSymbol(MessageOutputTypeRA enum_member) {
        return (OutputSymbol) alphabet.getPSymbol(enum_member);
    }

    protected PSymbolInstance instanceAfterCheck(boolean successfulCheck, OutputSymbol baseSymbol,
            DataValue<?>... values) {
        return successfulCheck ? instanceFromOutputSymbol(baseSymbol, values) : null;
    }

    protected PSymbolInstance instanceFromOutputSymbol(OutputSymbol baseSymbol, DataValue<?>... values) {
        return new PSymbolInstance(baseSymbol, values);
    }

    @Override
    protected PSymbolInstance buildOutput(String name, List<EdhocProtocolMessage> messages) {
        throw new UnsupportedOperationException("Unsupported output builder arguments");
    }
}
