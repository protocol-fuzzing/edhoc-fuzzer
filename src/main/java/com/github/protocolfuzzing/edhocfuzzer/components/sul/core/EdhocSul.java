package com.github.protocolfuzzing.edhocfuzzer.components.sul.core;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulClientConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.ProtocolVersion;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.ClientMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.ServerMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.ClientMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.ServerMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers.EdhocInputMapper;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers.EdhocOutputMapper;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInput;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputBuilder;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputChecker;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.MessageOutputType;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.AbstractSul;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.Mapper;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.config.MapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.InputMapper;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.MapperComposer;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.mappers.OutputMapper;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.config.CoapConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EdhocSul extends AbstractSul<EdhocInput, EdhocOutput, EdhocExecutionContext> {
    private static final Logger LOGGER = LogManager.getLogger();
    protected EdhocExecutionContext edhocExecutionContext;
    protected ProtocolVersion protocolVersion;
    protected Long originalTimeout;
    protected EdhocMapperState edhocMapperState;
    protected EdhocMapperConnector edhocMapperConnector;
    protected boolean serverWaitForInitialMessageDone;

    public EdhocSul(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        super(sulConfig, cleanupTasks);
        this.protocolVersion = ((EdhocMapperConfig) sulConfig.getMapperConfig()).getProtocolVersion();
        this.originalTimeout = sulConfig.getResponseWait();
    }

    public EdhocSul initialize() {
        try {
            // Adds also the californium standard configuration
            EdhocMapperConnectionConfig mapperConnectionConfig = new EdhocMapperConnectionConfig(
                    sulConfig.getMapperConfig().getMapperConnectionConfigInputStream());

            sulConfig.applyDelegate(mapperConnectionConfig);

            // Warn about possible retransmissions
            Long coapAckTimeout = mapperConnectionConfig.getConfiguration().get(
                CoapConfig.ACK_TIMEOUT, TimeUnit.MILLISECONDS);

            if (originalTimeout > coapAckTimeout) {
                LOGGER.warn("Found COAP.ACK_TIMEOUT ({} ms) < responseWait ({} ms)", coapAckTimeout, originalTimeout);
                LOGGER.warn("Retransmissions may occur implicitly and may affect the learned model's correctness");
                LOGGER.warn("To avoid them: [COAP.ACK_TIMEOUT > longest wait time] or [COAP.MAX_RETRANSMIT = 0]");
            }
        } catch (IOException e) {
            LOGGER.error("Exception occurred: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // The connector uses the californium standard configuration
        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) sulConfig.getMapperConfig();
        if (sulConfig.isFuzzingClient()){
            this.edhocMapperConnector = new ServerMapperConnector(edhocMapperConfig.getHostCoapUri(),
                    edhocMapperConfig.getEdhocCoapResource(), edhocMapperConfig.getAppCoapResource(),
                    originalTimeout);
        } else {
            this.edhocMapperConnector = new ClientMapperConnector(edhocMapperConfig.getEdhocCoapUri(),
                    edhocMapperConfig.getAppCoapUri(), this.originalTimeout);
        }

        this.mapper = buildMapper(sulConfig.getMapperConfig(), this.edhocMapperConnector);

        return this;
    }

    protected Mapper<EdhocInput, EdhocOutput, EdhocExecutionContext> buildMapper(MapperConfig mapperConfig, EdhocMapperConnector edhocMapperConnector) {
        return new EdhocMapperComposer (
                new EdhocInputMapper(mapperConfig,  new EdhocOutputChecker(), edhocMapperConnector),
                new EdhocOutputMapper(mapperConfig, new EdhocOutputBuilder(), new EdhocOutputChecker(), edhocMapperConnector)
        );
    }

    @Override
    public void pre() {
        LOGGER.debug("SUL 'pre' start");

        // mapper config
        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) sulConfig.getMapperConfig();

        if (sulConfig.isFuzzingClient()) {
            ServerMapperConnector serverMapperConnector = (ServerMapperConnector) edhocMapperConnector;
            edhocMapperState = new ServerMapperState(protocolVersion, edhocMapperConfig, cleanupTasks).initialize(serverMapperConnector);

            serverWaitForInitialMessageDone = false;
            cleanupTasks.submit(serverMapperConnector::shutdown);

            EdhocSulClientConfig edhocSulClientConfig = (EdhocSulClientConfig) sulConfig;
            long clientWait = edhocSulClientConfig.getClientWait();
            if (clientWait > 0) {
                try {
                    Thread.sleep(clientWait);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted 'clientWait' sleep for {} ms", clientWait);
                }
            }
        } else {
            ClientMapperConnector clientMapperConnector = (ClientMapperConnector) edhocMapperConnector;
            edhocMapperState = new ClientMapperState(protocolVersion, edhocMapperConfig, cleanupTasks).initialize(clientMapperConnector);
        }

        this.edhocExecutionContext = new EdhocExecutionContext(edhocMapperState);

        long startWait = sulConfig.getStartWait();
        if (startWait > 0) {
            try {
                Thread.sleep(startWait);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted 'startWait' sleep for {} ms", startWait);
            }
        }

        LOGGER.debug("SUL 'pre' end");
    }

    @Override
    public void post() {
        LOGGER.debug("SUL 'post' start");
        LOGGER.debug("SUL 'post' end");
    }

    @Override
    public EdhocOutput step(EdhocInput abstractInput) {
        // In case of server mapper, wait for initial message from client
        serverWaitForInitialMessage();

        LOGGER.debug("SUL 'step' start");

        edhocExecutionContext.addStepContext();

        if (!edhocExecutionContext.isExecutionEnabled()) {
            return ((EdhocMapperComposer) mapper).getOutputMapper().disabled();
        }

        EdhocOutput abstractOutput = executeInput(abstractInput, mapper);

        if (mapper.getOutputChecker().isDisabled(abstractOutput) || !edhocExecutionContext.isExecutionEnabled()) {
            // this should lead to a disabled sink state
            edhocExecutionContext.disableExecution();
        }

        LOGGER.debug("SUL 'step' end");

        return abstractOutput;
    }

    protected EdhocOutput executeInput(EdhocInput abstractInput, Mapper<EdhocInput, EdhocOutput, EdhocExecutionContext> mapper) {
        boolean timeoutChanged = false;

        // handle timeout from extendedWait and from inputResponse
        if (abstractInput.getExtendedWait() != null) {
            edhocMapperConnector.setTimeout(originalTimeout + abstractInput.getExtendedWait());
            timeoutChanged = true;
        } else if (sulConfig.getInputResponseTimeout() != null &&
                sulConfig.getInputResponseTimeout().containsKey(abstractInput.getName())) {
            edhocMapperConnector.setTimeout(sulConfig.getInputResponseTimeout().get(abstractInput.getName()));
            timeoutChanged = true;
        }

        EdhocOutput abstractOutput = mapper.execute(abstractInput, edhocExecutionContext);

        // reset timeout
        if (timeoutChanged) {
            edhocMapperConnector.setTimeout(originalTimeout);
        }
        return abstractOutput;
    }

    protected void serverWaitForInitialMessage() {
        boolean isServer = !edhocMapperState.isCoapClient();
        boolean isResponder = !edhocMapperState.getEdhocSessionPersistent().isInitiator();
        MessageOutputType expectedMessageType = isResponder ?
                MessageOutputType.EDHOC_MESSAGE_1 :
                MessageOutputType.COAP_EMPTY_MESSAGE;

        if (!isServer || serverWaitForInitialMessageDone) {
            return;
        }

        EdhocMapperComposer mapperComposer = (EdhocMapperComposer) mapper;
        ServerMapperConnector serverMapperConnector = (ServerMapperConnector) edhocMapperConnector;
        EdhocOutputChecker edhocOutputChecker = (EdhocOutputChecker) mapperComposer.getOutputChecker();

        serverMapperConnector.waitForClientMessage();
        EdhocOutput abstractOutput = mapperComposer.getOutputMapper().receiveOutput(edhocExecutionContext);
        boolean isExpectedMessage = edhocOutputChecker.isMessage(abstractOutput, expectedMessageType);

        if (!isExpectedMessage) {
            throw new RuntimeException("After initial wait, instead of " + expectedMessageType + ", received " +
                    abstractOutput.getName());
        }

        LOGGER.debug("Received {} from client", expectedMessageType);
        serverWaitForInitialMessageDone = true;
    }


    protected static class EdhocMapperComposer extends MapperComposer<EdhocInput, EdhocOutput, EdhocProtocolMessage, EdhocExecutionContext, EdhocMapperState> {
        public EdhocMapperComposer(
            InputMapper<EdhocInput, EdhocOutput, EdhocProtocolMessage, EdhocExecutionContext> inputMapper,
            OutputMapper<EdhocOutput, EdhocProtocolMessage, EdhocExecutionContext> outputMapper) {
            super(inputMapper, outputMapper);
        }
    }
}
