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
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputCheckerRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.MessageOutputType;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.OutputMapper;
import de.learnlib.ralib.data.DataType;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.ParameterizedSymbol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class EdhocOutputMapperRA extends OutputMapper<EdhocOutputRA, EdhocProtocolMessage, EdhocExecutionContextRA> {
    private static final Logger LOGGER = LogManager.getLogger();

    EdhocMapperConnector edhocMapperConnector;

    public EdhocOutputMapperRA(MapperConfig mapperConfig, EdhocOutputBuilderRA edhocOutputBuilder,
            EdhocOutputCheckerRA edhocOutputChecker, EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig, edhocOutputBuilder, edhocOutputChecker);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    public EdhocOutputRA receiveOutput(EdhocExecutionContextRA context) {
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

            OutputSymbol base = new OutputSymbol(MessageOutputType.UNSUPPORTED_MESSAGE.name(), new DataType[] {});
            return edhocOutputRA(base);
        } catch (UnsuccessfulMessageException e) {
            // special output to demonstrate that the received message evoked an error
            // in a middle layer and did not reach the upper resource in the case of
            // server mapper

            OutputSymbol base = new OutputSymbol(MessageOutputType.UNSUPPORTED_MESSAGE.name(), new DataType[] {});
            return edhocOutputRA(base);
        }

        EdhocOutputRA abstractOutput;

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

    protected EdhocOutputRA appOutput(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        String messageType = edhocMapperState.isCoapClient() ? "response" : "request";
        DataType T_CI = new DataType("C_I", Integer.class);

        if (edhocMapperConnector.receivedMsg3WithOscoreApp()) {
            // received Message3_OSCORE_APP, from which application data propagated and
            // decrypted
            LOGGER.info("EDHOC_MESSAGE_3_OSCORE_APP | OSCORE_APP_MESSAGE ({}): {} ~ {}",
                    messageType, EdhocUtil.byteArrayToString(responsePayload),
                    new String(responsePayload, StandardCharsets.UTF_8));

            Integer parameter = EdhocUtil
                    .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
            OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_3_OSCORE_APP.name(), T_CI);

            LOGGER.info("Reading as EDHOC Message 3 Oscore App, DataValue: " + parameter);
            DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);

            return edhocOutputRA(base, value);
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

            OutputSymbol base = new OutputSymbol(MessageOutputType.OSCORE_APP_MESSAGE.name(), new DataType[] {});
            return edhocOutputRA(base);
        }

        return null;
    }

    protected EdhocOutputRA edhocOutputRA(EdhocMapperState edhocMapperState, byte[] responsePayload) {
        MessageProcessorPersistent messageProcessorPersistent = new MessageProcessorPersistent(edhocMapperState);
        boolean ok;
        DataType T_CI = new DataType("C_I", Integer.class);

        switch (messageProcessorPersistent.messageTypeFromStructure(responsePayload)) {
            case EDHOC_ERROR_MESSAGE -> {
                ok = messageProcessorPersistent.readErrorMessage(responsePayload);

                OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_ERROR_MESSAGE.name(), new DataType[] {});
                return edhocOutputAfterCheck(ok, base, new DataValue<?>[] {});
            }

            case EDHOC_MESSAGE_1 -> {
                ok = messageProcessorPersistent.readMessage1(responsePayload);

                Integer parameter = EdhocUtil
                        .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_1.name(), T_CI);
                LOGGER.info("Reading as Message 1, DataValue: " + parameter);
                DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);
                return edhocOutputAfterCheck(ok, base, value);
            }

            case EDHOC_MESSAGE_2 -> {
                ok = messageProcessorPersistent.readMessage2(responsePayload);

                Integer parameter = EdhocUtil
                        .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_2.name(), T_CI);
                LOGGER.info("Reading as Message 2, DataValue: " + parameter);
                DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);
                return edhocOutputAfterCheck(ok, base, value);
            }

            case EDHOC_MESSAGE_3_OR_4 -> {
                // message may be 3 or 4
                LOGGER.info("Reading as EDHOC Message 3 or 4");
                ok = messageProcessorPersistent.readMessage3(responsePayload);

                if (ok) {
                    Integer parameter = EdhocUtil
                            .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                    OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_3.name(), T_CI);
                    LOGGER.info("Reading as Message 3, DataValue: " + parameter);
                    DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);
                    return edhocOutputRA(base, value);
                }

                ok = messageProcessorPersistent.readMessage4(responsePayload);
                Integer parameter = EdhocUtil
                        .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_4.name(), T_CI);
                LOGGER.info("Reading as Message 4, DataValue: " + parameter);
                DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);
                return edhocOutputAfterCheck(ok, base, value);
            }

            case EDHOC_MESSAGE_2_OR_3_OR_4 -> {
                // message may be 2 or 3 or 4
                LOGGER.info("Reading as EDHOC Message 2 or 3 or 4");
                ok = messageProcessorPersistent.readMessage2(responsePayload);
                if (ok) {
                    Integer parameter = EdhocUtil
                            .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                    OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_2.name(), T_CI);
                    LOGGER.info("Reading as Message 2, DataValue: " + parameter);
                    DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);
                    return edhocOutputRA(base, value);
                }

                ok = messageProcessorPersistent.readMessage3(responsePayload);
                if (ok) {
                    Integer parameter = EdhocUtil
                            .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                    OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_3.name(), T_CI);
                    LOGGER.info("Reading as Message 3, DataValue: " + parameter);
                    DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);
                    return edhocOutputRA(base, value);
                }

                ok = messageProcessorPersistent.readMessage4(responsePayload);
                Integer parameter = EdhocUtil
                        .bytesToInt(edhocMapperState.getEdhocSessionPersistent().getConnectionId());
                OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_MESSAGE_4.name(), T_CI);
                LOGGER.info("Reading as Message 4, DataValue: " + parameter);
                DataValue<Integer> value = new DataValue<Integer>(T_CI, parameter);
                return edhocOutputAfterCheck(ok, base, value);
            }

            default -> {
                return null;
            }
        }
    }

    protected EdhocOutputRA coapOutput(EdhocMapperState edhocMapperState, byte[] responsePayload) {
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
            OutputSymbol base = new OutputSymbol(MessageOutputType.COAP_EMPTY_MESSAGE.name(), new DataType[] {});
            return edhocOutputRA(base);
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
            OutputSymbol base = new OutputSymbol(MessageOutputType.COAP_APP_MESSAGE.name(), new DataType[] {});
            return edhocOutputRA(base);
        }

        // if payload was not empty then a coap message is received
        // because no other transport protocol than coap is supported yet
        OutputSymbol base = new OutputSymbol(MessageOutputType.COAP_MESSAGE.name(), new DataType[] {});
        return edhocOutputAfterCheck(responsePayload != null, base, new DataValue<?>[] {});
    }

    protected EdhocOutputRA coapError() {
        if (((EdhocMapperConfig) mapperConfig).isCoapErrorAsEdhocError()) {
            OutputSymbol base = new OutputSymbol(MessageOutputType.EDHOC_ERROR_MESSAGE.name(), new DataType[] {});
            return edhocOutputRA(base);
        } else {
            OutputSymbol base = new OutputSymbol(MessageOutputType.COAP_ERROR_MESSAGE.name(), new DataType[] {});
            return edhocOutputRA(base);
        }
    }

    protected EdhocOutputRA edhocOutputAfterCheck(boolean successfulCheck, OutputSymbol baseSymbol,
            DataValue<?>... values) {
        return successfulCheck ? edhocOutputRA(baseSymbol, values) : null;
    }

    protected EdhocOutputRA edhocOutputRA(OutputSymbol baseSymbol, DataValue<?>... values) {
        return new EdhocOutputRA(baseSymbol, values);
    }

    protected EdhocOutputRA buildOutput(List<EdhocProtocolMessage> messages, ParameterizedSymbol baseSymbol,
            DataValue<?>... parameterValues) {
        return new EdhocOutputRA(messages, baseSymbol, parameterValues);
    }

    @Override
    protected EdhocOutputRA buildOutput(String name, List<EdhocProtocolMessage> messages) {
        throw new UnsupportedOperationException("Unsupported output builder arguments");
    }
}
