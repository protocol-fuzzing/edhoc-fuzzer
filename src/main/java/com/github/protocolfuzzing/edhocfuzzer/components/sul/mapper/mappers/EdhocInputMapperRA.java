package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocUtil;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.CoapAppMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.CoapEmptyMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.EdhocErrorMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.OscoreAppMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator.EdhocMessage1;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator.EdhocMessage3;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator.EdhocMessage3OscoreApp;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.responder.EdhocMessage2;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.responder.EdhocMessage4;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.MessageInputType;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputCheckerRA;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.InputMapperRA;
import de.learnlib.ralib.words.InputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;
import de.learnlib.ralib.words.ParameterizedSymbol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.EnumMap;

public class EdhocInputMapperRA extends InputMapperRA<PSymbolInstance, EdhocProtocolMessage, EdhocExecutionContextRA> {
    EdhocMapperConnector edhocMapperConnector;

    private static Logger LOGGER = LogManager.getLogger();
    // protected DataType T_CI = new DataType("C_I", Integer.class);

    protected EnumMap<MessageInputType, Long> timeoutMap = new EnumMap<MessageInputType, Long>(MessageInputType.class);

    public EdhocInputMapperRA(MapperConfig mapperConfig, EdhocOutputCheckerRA outputChecker,
            EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig, outputChecker);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    public void sendMessage(EdhocProtocolMessage message, EdhocExecutionContextRA context) {
        if (message == null) {
            throw new RuntimeException("Null message provided to EdhocInputMapper in sendMessage");
        }

        // enable or disable content format
        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) mapperConfig;
        int contentFormat = edhocMapperConfig.useContentFormat() ? message.getContentFormat()
                : MediaTypeRegistry.UNDEFINED;

        edhocMapperConnector.send(message.getPayload(), message.getPayloadType(), message.getMessageCode(),
                contentFormat);
    }

    @Override
    public void preSendUpdate(PSymbolInstance input, EdhocExecutionContextRA context) {
        String symbolName = input.getBaseSymbol().getName();
        EdhocSessionPersistent session = context.getState().getEdhocSessionPersistent();

        switch (MessageInputType.valueOf(symbolName)) {
            case EDHOC_MESSAGE_1:
                if (session.isInitiator()) {
                    // Initiator by sending message 1 starts a new key exchange session
                    // so previous session state must be cleaned unless reset is disabled
                    session.resetIfEnabled();
                }
                updateConnectionId(session, input);
                break;

            case EDHOC_MESSAGE_2:
            case EDHOC_MESSAGE_3:
            case EDHOC_MESSAGE_4:
            case OSCORE_APP_MESSAGE:
                updateConnectionId(session, input);
                break;

            case EDHOC_MESSAGE_3_OSCORE_APP:
                updateConnectionId(session, input);
                // construct Message3 in order to store it in session 'message3' field,
                // derive new oscore context and make Message3 available to oscore layer
                new MessageProcessorPersistent(context.getState()).writeMessage3();
                break;

            case COAP_APP_MESSAGE:
            case COAP_EMPTY_MESSAGE:
            case EDHOC_ERROR_MESSAGE:
                break;
        }
    }

    @Override
    public EdhocProtocolMessage generateProtocolMessage(PSymbolInstance input, EdhocExecutionContextRA context) {
        ParameterizedSymbol baseSymbol = input.getBaseSymbol();
        String symbolName = baseSymbol.getName();
        if (baseSymbol instanceof InputSymbol) {
            // We can construct this here since the switch should always dispatch to only
            // one instance.
            MessageProcessorPersistent messageProcessor = new MessageProcessorPersistent(context.getState());
            switch (MessageInputType.valueOf(symbolName)) {
                case EDHOC_MESSAGE_1:
                    return new EdhocMessage1(messageProcessor);

                case EDHOC_MESSAGE_2:
                    return new EdhocMessage2(messageProcessor);

                case EDHOC_MESSAGE_3:
                    return new EdhocMessage3(messageProcessor);

                case EDHOC_MESSAGE_3_OSCORE_APP:
                    return new EdhocMessage3OscoreApp(messageProcessor);

                case EDHOC_MESSAGE_4:
                    return new EdhocMessage4(messageProcessor);

                case OSCORE_APP_MESSAGE:
                    return new OscoreAppMessage(messageProcessor);

                case EDHOC_ERROR_MESSAGE:
                    return new EdhocErrorMessage(messageProcessor);

                case COAP_APP_MESSAGE:
                    return new CoapAppMessage(messageProcessor);

                case COAP_EMPTY_MESSAGE:
                    return new CoapEmptyMessage(messageProcessor);
            }
        }

        throw new RuntimeException(
                "Input mapper can only map input symbols: " + baseSymbol + " is not an InputSymbol.");

    }

    @Override
    public void postReceiveUpdate(PSymbolInstance input, PSymbolInstance output,
            OutputChecker<PSymbolInstance> outputChecker, EdhocExecutionContextRA context) {
    }

    @Override
    public void postSendUpdate(PSymbolInstance input, EdhocExecutionContextRA context) {
    }

    /*
     * TODO This is bad in multiple ways:
     * - We need to have access to the datatype, which means defining it multiple
     * times. For teachers, EdhocInputRA and the EdhocOutputMapperRA.
     * - If the C_I is a bytestring it is unclear if use of a mapper to convert from
     * a randomly selected integer in the learner to a corresponding bytestring is
     * possible.
     */
    public void updateConnectionId(EdhocSessionPersistent session, PSymbolInstance input) {

        LOGGER.info("Running updateConnectionId method");
        LOGGER.info("Current ConnectionId: " + EdhocUtil.bytesToInt(session.getConnectionId()));

        // for (DataValue<?> dv : input.getParameterValues()) {

        // LOGGER.info("Datavalue: " + dv.toString());
        // if (dv.getType().equals(T_CI)) {
        // CBORObject value = CBORObject.FromObject(dv.getId());
        // LOGGER.info("CBORObject version of DataValue id: " + value.toString());

        // // session.setConnectionId(value.EncodeToBytes());
        // LOGGER.info("ConnectionId after set: " +
        // EdhocUtil.bytesToInt(session.getConnectionId()));
        // }
        // }
    }

    public long getTimeoutForSymbol(PSymbolInstance input) {
        String baseSymbolName = input.getBaseSymbol().getName();
        MessageInputType key = MessageInputType.valueOf(baseSymbolName);
        return timeoutMap.getOrDefault(key, 0L);
    }

    public void setTimeoutForSymbol(PSymbolInstance input, long timeout) {
        String baseSymbolName = input.getBaseSymbol().getName();
        MessageInputType key = MessageInputType.valueOf(baseSymbolName);
        timeoutMap.put(key, timeout);
    }
}
