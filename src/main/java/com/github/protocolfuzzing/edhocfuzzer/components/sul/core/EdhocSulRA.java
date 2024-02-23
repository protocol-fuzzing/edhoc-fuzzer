package com.github.protocolfuzzing.edhocfuzzer.components.sul.core;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.config.EdhocSulClientConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.ClientMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.ServerMapperConnector;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.ClientMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.ServerMapperState;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers.EdhocInputMapper;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers.EdhocMapperComposer;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers.EdhocMapperComposerRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.mappers.EdhocOutputMapper;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs.EdhocInputRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputBuilder;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputChecker;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutputRA;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.MessageOutputType;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.AbstractSul;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.SulAdapter;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.config.SulConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.core.sulwrappers.DynamicPortProvider;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.config.CoapConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EdhocSulRA implements AbstractSul<EdhocInputRA, EdhocOutputRA, EdhocExecutionContext> {
    private static final Logger LOGGER = LogManager.getLogger();

    protected SulConfig sulConfig;
    protected CleanupTasks cleanupTasks;
    protected EdhocMapperConfig edhocMapperConfig;
    protected EdhocMapperComposerRA edhocMapperComposer;
    protected EdhocExecutionContext edhocExecutionContext;
    protected Long originalTimeout;
    protected EdhocMapperState edhocMapperState;
    protected EdhocMapperConnector edhocMapperConnector;
    protected boolean serverWaitForInitialMessageDone;

    public EdhocSulRA(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        this.sulConfig = sulConfig;
        this.cleanupTasks = cleanupTasks;
        this.edhocMapperConfig = (EdhocMapperConfig) sulConfig.getMapperConfig();
        this.originalTimeout = sulConfig.getResponseWait();
    }

    public EdhocSulRA initialize() {
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
        if (sulConfig.isFuzzingClient()) {
            this.edhocMapperConnector = new ServerMapperConnector(edhocMapperConfig.getHostCoapUri(),
                    edhocMapperConfig.getEdhocCoapResource(), edhocMapperConfig.getAppCoapResource(),
                    originalTimeout);
        } else {
            this.edhocMapperConnector = new ClientMapperConnector(edhocMapperConfig.getEdhocCoapUri(),
                    edhocMapperConfig.getAppCoapUri(), this.originalTimeout);
        }

        this.edhocMapperComposer = new EdhocMapperComposerRA(
                new EdhocInputMapper(edhocMapperConfig, new EdhocOutputChecker(), edhocMapperConnector),
                new EdhocOutputMapper(edhocMapperConfig, new EdhocOutputBuilder(), new EdhocOutputChecker(),
                        edhocMapperConnector));

        return this;
    }

    @Override
    public SulConfig getSulConfig() {
        return sulConfig;
    }

    @Override
    public CleanupTasks getCleanupTasks() {
        return cleanupTasks;
    }

    @Override
    public EdhocMapperComposerRA getMapper() {
        return edhocMapperComposer;
    }

    @Override
    public void setDynamicPortProvider(DynamicPortProvider dynamicPortProvider) {
        throw new RuntimeException("No dynamic port provider available");
    }

    @Override
    public DynamicPortProvider getDynamicPortProvider() {
        throw new RuntimeException("No dynamic port provider available");
    }

    @Override
    public SulAdapter getSulAdapter() {
        throw new RuntimeException("No sul adapter available");
    }

    @Override
    public void pre() {
        LOGGER.debug("SUL 'pre' start");

        if (sulConfig.isFuzzingClient()) {
            ServerMapperConnector serverMapperConnector = (ServerMapperConnector) edhocMapperConnector;
            edhocMapperState = new ServerMapperState(edhocMapperConfig, cleanupTasks).initialize(serverMapperConnector);

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
            edhocMapperState = new ClientMapperState(edhocMapperConfig, cleanupTasks).initialize(clientMapperConnector);
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
    public EdhocOutputRA step(EdhocInputRA abstractInput) {
        // In case of server mapper, wait for initial message from client
        serverWaitForInitialMessage();

        LOGGER.debug("SUL 'step' start");

        edhocExecutionContext.addStepContext();

        if (!edhocExecutionContext.isExecutionEnabled()) {
            return edhocMapperComposer.getOutputMapper().disabled();
        }

        EdhocOutputRA abstractOutput = executeInput(abstractInput);

        if (edhocMapperComposer.getOutputChecker().isDisabled(abstractOutput)
                || !edhocExecutionContext.isExecutionEnabled()) {
            // this should lead to a disabled sink state
            edhocExecutionContext.disableExecution();
        }

        LOGGER.debug("SUL 'step' end");

        return abstractOutput;
    }

    protected EdhocOutputRA executeInput(EdhocInputRA abstractInput) {
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

        EdhocOutputRA abstractOutput = edhocMapperComposer.execute(abstractInput, edhocExecutionContext);

        // reset timeout
        if (timeoutChanged) {
            edhocMapperConnector.setTimeout(originalTimeout);
        }
        return abstractOutput;
    }

    protected void serverWaitForInitialMessage() {
        boolean isServer = !edhocMapperState.isCoapClient();
        boolean isResponder = !edhocMapperState.getEdhocSessionPersistent().isInitiator();
        MessageOutputType expectedMessageType = isResponder ? MessageOutputType.EDHOC_MESSAGE_1
                : MessageOutputType.COAP_EMPTY_MESSAGE;

        if (!isServer || serverWaitForInitialMessageDone) {
            return;
        }

        ServerMapperConnector serverMapperConnector = (ServerMapperConnector) edhocMapperConnector;
        EdhocOutputChecker edhocOutputChecker = edhocMapperComposer.getOutputChecker();

        serverMapperConnector.waitForClientMessage();
        EdhocOutputRA abstractOutput = edhocMapperComposer.getOutputMapper().receiveOutput(edhocExecutionContext);
        boolean isExpectedMessage = edhocOutputChecker.isMessage(abstractOutput, expectedMessageType);

        if (!isExpectedMessage) {
            throw new RuntimeException("After initial wait, instead of " + expectedMessageType + ", received " +
                    abstractOutput.getName());
        }

        LOGGER.debug("Received {} from client", expectedMessageType);
        serverWaitForInitialMessageDone = true;
    }

}
