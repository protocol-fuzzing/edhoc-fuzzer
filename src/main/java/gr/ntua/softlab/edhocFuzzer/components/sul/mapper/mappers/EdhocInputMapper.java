package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocProtocolMessage;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.MapperConnector;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.InputMapper;

public class EdhocInputMapper extends InputMapper {
    MapperConnector mapperConnector;

    public EdhocInputMapper(MapperConnector mapperConnector) {
        this.mapperConnector = mapperConnector;
    }

    @Override
    protected void sendMessage(ProtocolMessage message, ExecutionContext context) {
        EdhocProtocolMessage edhocProtocolMessage = (EdhocProtocolMessage) message;
        mapperConnector.send(edhocProtocolMessage.getCBORSequence());
    }
}
