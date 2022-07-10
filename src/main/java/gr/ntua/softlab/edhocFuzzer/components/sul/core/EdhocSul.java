package gr.ntua.softlab.edhocFuzzer.components.sul.core;

import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.config.EdhocMapperConnectionConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulClientConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulServerConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.ServerMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.ClientMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.ClientMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.ServerMapperState;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocInputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocOutputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.EdhocOutputChecker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.AbstractSul;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContextStepped;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.State;
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
    protected EdhocMapperConnector edhocMapperConnector;

    public EdhocSul(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        super(sulConfig, cleanupTasks);

        try {
            EdhocMapperConnectionConfig mapperConnectionConfig = new EdhocMapperConnectionConfig(
                    sulConfig.getMapperConnectionConfigInputStream());

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

        if (sulConfig.isFuzzingClient()){
            this.edhocMapperConnector = new ServerMapperConnector();
        } else {
            String coapHostURI = ((EdhocSulServerConfig) sulConfig).getCoapHostURI();
            this.edhocMapperConnector = new ClientMapperConnector(coapHostURI, originalTimeout);
        }

        this.mapper = buildMapper(sulConfig.getMapperConfig(), this.edhocMapperConnector);
    }

    protected Mapper buildMapper(MapperConfig mapperConfig, EdhocMapperConnector edhocMapperConnector) {
        return new MapperComposer(
                new EdhocInputMapper(edhocMapperConnector),
                new EdhocOutputMapper(mapperConfig, edhocMapperConnector),
                new EdhocOutputChecker()
        );
    }

    @Override
    public void pre() {
        LOGGER.debug("Executing SUL 'pre'");

        // state to pass on ExecutionContextStepped
        State state;

        if (sulConfig.isFuzzingClient()) {
            EdhocSulClientConfig config = (EdhocSulClientConfig) sulConfig;
            state = new ServerMapperState();
            long clientWait = config.getClientWait();
            if (clientWait > 0) {
                try {
                    Thread.sleep(clientWait);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted 'pre' sleep for {} ms", clientWait);
                }
            }
        } else {
            EdhocSulServerConfig config = (EdhocSulServerConfig) sulConfig;
            String coapHostURI = config.getCoapHostURI();
            state = new ClientMapperState(coapHostURI, config.getAuthenticationFileConfig());
        }

        this.executionContextStepped = new ExecutionContextStepped(state);
    }

    @Override
    public void post() {
        LOGGER.debug("Executing SUL 'post'");
        long startWait = sulConfig.getStartWait();
        if (startWait > 0) {
            try {
                Thread.sleep(startWait);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted 'post' sleep for {} ms", startWait);
            }
        }
    }

    @Override
    public AbstractOutput step(AbstractInput abstractInput) {
        LOGGER.debug("Executing SUL 'step'");
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

}
