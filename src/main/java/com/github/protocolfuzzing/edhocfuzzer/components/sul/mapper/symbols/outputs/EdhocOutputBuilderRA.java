package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputBuilder;

public class EdhocOutputBuilderRA implements OutputBuilder<EdhocOutputRA> {
    @Override
    public EdhocOutputRA buildOutput(String name) {
        return new EdhocOutputRA(name);
    }
}
