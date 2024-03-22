package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocSessionPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator.EdhocMessage3;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContextRA;
import de.learnlib.ralib.data.DataValue;
import de.learnlib.ralib.words.ParameterizedSymbol;

public class EdhocMessage3InputRA extends EdhocInputRA {

    public EdhocMessage3InputRA(ParameterizedSymbol baseSymbol, DataValue<?>[] parameterValues) {
        super(baseSymbol, parameterValues);
    }

    @Override
    public void preSendUpdate(EdhocExecutionContextRA context) {
        EdhocSessionPersistent session = context.getState().getEdhocSessionPersistent();

        updateConnectionId(session);
    }

    @Override
    public EdhocProtocolMessage generateProtocolMessage(EdhocExecutionContextRA context) {
        return new EdhocMessage3(new MessageProcessorPersistent(context.getState()));
    }

    @Override
    public Enum<MessageInputType> getInputType() {
        return MessageInputType.EDHOC_MESSAGE_3;
    }
}
