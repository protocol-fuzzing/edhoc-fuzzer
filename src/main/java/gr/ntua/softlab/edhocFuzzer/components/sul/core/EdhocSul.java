package gr.ntua.softlab.edhocFuzzer.components.sul.core;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulClientConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient.ServerMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulServer.ClientMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.ClientMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.ServerMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocInputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocOutputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs.EdhocOutputChecker;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs.EdhocOutputType;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.AbstractSul;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContextStepped;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.MapperComposer;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.config.CoapConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EdhocSul extends AbstractSul {
    private static final Logger LOGGER = LogManager.getLogger(EdhocSul.class);
    protected ExecutionContextStepped executionContextStepped;
    protected Long originalTimeout;
    protected EdhocMapperState edhocMapperState;
    protected EdhocMapperConnector edhocMapperConnector;
    protected boolean serverWaitForMessage1Done;

    public EdhocSul(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        super(sulConfig, cleanupTasks);

        try {
            EdhocMapperConnectionConfig mapperConnectionConfig = new EdhocMapperConnectionConfig(
                    sulConfig.getMapperConfig().getMapperConnectionConfigInputStream());

            // timeout taken from configuration file
            Long coapExchangeLifetime = mapperConnectionConfig.getConfiguration().get(
                    CoapConfig.EXCHANGE_LIFETIME, TimeUnit.MILLISECONDS);

            // timeout taken from responseWait in command-line
            Long responseWait = sulConfig.getResponseWait();

            if (responseWait < coapExchangeLifetime) {
                LOGGER.warn("Provided responseWait ({} ms) is less than COAP.EXCHANGE_LIFETIME ({} ms) in "
                                + "mapper_connection config file", responseWait, coapExchangeLifetime);
            }

            this.originalTimeout = responseWait;

            // apply delegate
            sulConfig.applyDelegate(mapperConnectionConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) sulConfig.getMapperConfig();
        if (sulConfig.isFuzzingClient()){
            this.edhocMapperConnector = new ServerMapperConnector(edhocMapperConfig.getHostCoapUri(),
                    edhocMapperConfig.getEdhocCoapResource(), edhocMapperConfig.getAppGetCoapResource(),
                    originalTimeout);
        } else {
            this.edhocMapperConnector = new ClientMapperConnector(edhocMapperConfig.getEdhocCoapUri(),
                    edhocMapperConfig.getAppGetCoapUri(), originalTimeout);
        }

        this.mapper = buildMapper(sulConfig.getMapperConfig(), this.edhocMapperConnector);
    }

    protected Mapper buildMapper(MapperConfig mapperConfig, EdhocMapperConnector edhocMapperConnector) {
        return new MapperComposer(
                new EdhocInputMapper(mapperConfig,  new EdhocOutputChecker(), edhocMapperConnector),
                new EdhocOutputMapper(mapperConfig, edhocMapperConnector)
        );
    }

    @Override
    public void pre() {
        LOGGER.debug("SUL 'pre' start");

        // mapper config
        EdhocMapperConfig edhocMapperConfig = (EdhocMapperConfig) sulConfig.getMapperConfig();

        if (sulConfig.isFuzzingClient()) {
            ServerMapperConnector serverMapperConnector = (ServerMapperConnector) edhocMapperConnector;
            edhocMapperState = new ServerMapperState(edhocMapperConfig, serverMapperConnector);

            serverWaitForMessage1Done = false;
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
            edhocMapperState = new ClientMapperState(edhocMapperConfig, (ClientMapperConnector) edhocMapperConnector);
        }

        this.executionContextStepped = new ExecutionContextStepped(edhocMapperState);

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
    public AbstractOutput step(AbstractInput abstractInput) {
        // In case of server mapper, wait for initial message 1 from client
        serverWaitForMessage1();

        LOGGER.debug("SUL 'step' start");

        executionContextStepped.addStepContext();
        Mapper preferredMapper = abstractInput.getPreferredMapper(sulConfig);
        if (preferredMapper == null) {
            preferredMapper = this.mapper;
        }

        if (!executionContextStepped.isExecutionEnabled()) {
            return ((MapperComposer) this.mapper).getOutputMapper().disabled();
        }

        AbstractOutput abstractOutput = executeInput(abstractInput, preferredMapper);

        if (abstractOutput == AbstractOutput.disabled() || !executionContextStepped.isExecutionEnabled()) {
            // this should lead to a disabled sink state
            executionContextStepped.disableExecution();
        }

        LOGGER.debug("SUL 'step' end");

        return abstractOutput;
    }

    protected AbstractOutput executeInput(AbstractInput abstractInput, Mapper mapper) {
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

        AbstractOutput abstractOutput = mapper.execute(abstractInput, executionContextStepped);

        // reset timeout
        if (timeoutChanged) {
            edhocMapperConnector.setTimeout(originalTimeout);
        }
        return abstractOutput;
    }

    protected void serverWaitForMessage1() {
        boolean isServer = (edhocMapperState instanceof ServerMapperState)
                && (edhocMapperConnector instanceof ServerMapperConnector);

        if (!isServer || serverWaitForMessage1Done) {
            return;
        }

        MapperComposer mapperComposer = (MapperComposer) mapper;
        ServerMapperConnector serverMapperConnector = (ServerMapperConnector) edhocMapperConnector;
        EdhocOutputChecker edhocOutputChecker = (EdhocOutputChecker) mapperComposer.getAbstractOutputChecker();

        serverMapperConnector.waitForClientMessage();
        AbstractOutput abstractOutput = mapperComposer.getOutputMapper().receiveOutput(executionContextStepped);
        boolean isMessage1 = edhocOutputChecker.isMessage(abstractOutput, EdhocOutputType.EDHOC_MESSAGE_1);

        if (!isMessage1) {
            throw new RuntimeException("After initial waiting, instead of message 1, received " +
                    abstractOutput.getName());
        }

        LOGGER.info("Received Initial Message 1 from client");
        serverWaitForMessage1Done = true;
    }
}
