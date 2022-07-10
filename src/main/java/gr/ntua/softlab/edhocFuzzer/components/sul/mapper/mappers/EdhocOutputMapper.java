package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.mappers;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.EdhocMapperConnector;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context.EdhocMapperState;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.ExecutionContext;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.mappers.OutputMapper;
import org.eclipse.californium.edhoc.Constants;

public class EdhocOutputMapper extends OutputMapper {
    EdhocMapperConnector edhocMapperConnector;

    public EdhocOutputMapper(MapperConfig mapperConfig, EdhocMapperConnector edhocMapperConnector) {
        super(mapperConfig);
        this.edhocMapperConnector = edhocMapperConnector;
    }

    @Override
    public AbstractOutput receiveOutput(ExecutionContext context) {
        EdhocMapperState edhocMapperState = (EdhocMapperState) context.getState();
        EdhocSessionPersistent edhocSessionPersistent = (EdhocSessionPersistent) edhocMapperState.getEdhocSession();

        byte[] responsePayload = edhocMapperConnector.receive();

        if (responsePayload == null) {
            // in case of response after exception
            return socketClosed();
        } else if (responsePayload.length == 0) {
            // in case of timeout
            return timeout();
        } else if (!edhocMapperConnector.isLatestResponseSuccessful()) {
            // in case of error identified from the coap response
            return new AbstractOutput("EDHOC_ERROR_MESSAGE");
        }

        MessageProcessorPersistent messageProcessorPersistent = new MessageProcessorPersistent(edhocMapperState);

        int structuralMessageType = messageProcessorPersistent.messageTypeFromStructure(responsePayload, false);

        switch (structuralMessageType) {
            case Constants.EDHOC_ERROR_MESSAGE -> {
                return new AbstractOutput("EDHOC_ERROR_MESSAGE");
            }

            case Constants.EDHOC_MESSAGE_1 -> {
                boolean ok = messageProcessorPersistent.readMessage1(responsePayload, true);
                return abstractOutputAfterCheck(ok, "EDHOC_MESSAGE_1");
            }

            case Constants.EDHOC_MESSAGE_2 -> {
                boolean ok = messageProcessorPersistent.readMessage2(responsePayload, false,
                        edhocSessionPersistent.getConnectionId());
                return abstractOutputAfterCheck(ok, "EDHOC_MESSAGE_2");
            }

            case Constants.EDHOC_MESSAGE_3 -> {
                // message may be 3 or 4
                boolean ok = messageProcessorPersistent.readMessage3(responsePayload, false, null);
                if (ok) {
                    return new AbstractOutput("EDHOC_MESSAGE_3");
                }

                ok = messageProcessorPersistent.readMessage4(responsePayload, false,
                        edhocSessionPersistent.getConnectionId());
                return abstractOutputAfterCheck(ok, "EDHOC_MESSAGE_4");
            }

            default -> {
                return AbstractOutput.unknown();
            }
        }
    }

    protected AbstractOutput abstractOutputAfterCheck(boolean successfulCheck, String outputName) {
        return successfulCheck ? new AbstractOutput(outputName) : AbstractOutput.unknown();
    }

}
