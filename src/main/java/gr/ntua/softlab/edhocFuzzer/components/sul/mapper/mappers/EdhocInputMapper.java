package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocProtocolMessage;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.InputMapper;

public class EdhocInputMapper extends InputMapper {
    EdhocMapperConnector edhocMapperConnector;

    public EdhocInputMapper(EdhocMapperConnector edhocMapperConnector) {
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    protected void sendMessage(ProtocolMessage message, ExecutionContext context) {
        EdhocProtocolMessage edhocProtocolMessage = (EdhocProtocolMessage) message;
        edhocMapperConnector.send(edhocProtocolMessage.getPayload(), edhocProtocolMessage.getPayloadType(),
                edhocProtocolMessage.getCoapCode(), edhocProtocolMessage.getContentFormat());
    }
}
