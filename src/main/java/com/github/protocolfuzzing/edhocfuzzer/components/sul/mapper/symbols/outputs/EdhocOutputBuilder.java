package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputBuilder;

public class EdhocOutputBuilder extends OutputBuilder<EdhocOutput> {
    @Override
    public EdhocOutput buildOutputExact(String name) {
        return new EdhocOutput(name);
    }
}
