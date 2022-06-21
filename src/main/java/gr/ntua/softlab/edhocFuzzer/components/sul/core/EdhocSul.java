package gr.ntua.softlab.edhocFuzzer.components.sul.core;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocMapperConnectionConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulClientConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulServerConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.ConnectorToClient;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.ConnectorToServer;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.MapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocState;
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
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.MapperComposer;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class EdhocSul extends AbstractSul {
    private static final Logger LOGGER = LogManager.getLogger(EdhocSul.class);
    protected ExecutionContextStepped executionContextStepped;

    public EdhocSul(SulConfig sulConfig, CleanupTasks cleanupTasks) {
        super(sulConfig, cleanupTasks);

        try {
            sulConfig.applyDelegate(new EdhocMapperConnectionConfig(sulConfig.getMapperConnectionConfigInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // build mapper for this sul
        MapperConnector mapperConnector;
        if (sulConfig.isFuzzingClient()){
            mapperConnector = new ConnectorToClient(); // TODO
        } else {
            String uri = ((EdhocSulServerConfig) sulConfig).getCoapHostURI();
            mapperConnector = new ConnectorToServer(uri);
        }

        this.mapper = buildMapper(sulConfig.getMapperConfig(), mapperConnector);
    }

    protected Mapper buildMapper(MapperConfig mapperConfig, MapperConnector mapperConnector) {
        return new MapperComposer(
                new EdhocInputMapper(mapperConnector),
                new EdhocOutputMapper(mapperConfig, mapperConnector),
                new EdhocOutputChecker()
        );
    }

    @Override
    public void pre() {
        // TODO create edhocSession
        executionContextStepped = new ExecutionContextStepped(new EdhocState(null));

        // in case of state-fuzzing a client implementation
        // that means mapper behaves like a server
        if (sulConfig.isFuzzingClient()) {
            long clientWait = ((EdhocSulClientConfig) sulConfig).getClientWait();
            if (clientWait > 0) {
                try {
                    Thread.sleep(clientWait);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted 'pre' sleep for {} ms", clientWait);
                }
            }
        }
    }

    @Override
    public void post() {
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
        LOGGER.debug("mapper sent: {}", abstractInput);
        // TODO handle timeout from extendedWait and inputResponse
        AbstractOutput abstractOutput = mapper.execute(abstractInput, executionContextStepped);
        LOGGER.debug("mapper received: {}", abstractOutput);
        return abstractOutput;
    }

}
