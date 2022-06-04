package gr.ntua.softlab.edhocFuzzer.components.sul.core;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocMapperConnectionConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.config.EdhocSulServerConfig;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.ConnectorToClient;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.MapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.ConnectorToServer;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocInputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers.EdhocOutputMapper;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.EdhocOutputChecker;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.AbstractSul;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.Mapper;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.MapperComposer;
import gr.ntua.softlab.protocolStateFuzzer.utils.CleanupTasks;

import java.io.IOException;

public class EdhocSul extends AbstractSul {

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

    }

    @Override
    public void post() {

    }

    @Override
    public AbstractOutput step(AbstractInput abstractInput) {
        return null;
    }

}
