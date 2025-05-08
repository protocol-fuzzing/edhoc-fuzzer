package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.inputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context.EdhocExecutionContext;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs.EdhocOutput;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractInputXml;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;

public abstract class EdhocInput extends AbstractInputXml<EdhocOutput, EdhocProtocolMessage, EdhocExecutionContext> {
    public abstract Enum<MessageInputType> getInputType();

    @Override
    public void preSendUpdate(EdhocExecutionContext context) {}

    @Override
    public void postSendUpdate(EdhocExecutionContext context) {}

    @Override
    public void postReceiveUpdate(
        EdhocOutput output,
        OutputChecker<EdhocOutput> abstractOutputChecker,
        EdhocExecutionContext context) {}
}
