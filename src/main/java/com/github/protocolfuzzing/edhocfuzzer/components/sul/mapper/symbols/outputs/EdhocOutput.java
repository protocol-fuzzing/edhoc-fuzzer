package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.EdhocProtocolMessage;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractOutput;

import java.util.List;

public class EdhocOutput extends AbstractOutput<EdhocOutput, EdhocProtocolMessage> {
    public EdhocOutput(String name) {
        super(name);
    }

    public EdhocOutput(String name, List<EdhocProtocolMessage> messages) {
        super(name, messages);
    }

    @Override
    protected EdhocOutput buildOutput(String name) {
        return new EdhocOutput(name);
    }

    @Override
    protected EdhocOutput convertOutput() {
        return new EdhocOutput(name, messages);
    }
}
