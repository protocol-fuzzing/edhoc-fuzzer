package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputBuilder;
import de.learnlib.ralib.words.PSymbolInstance;

public class EdhocOutputBuilderRA implements OutputBuilder<PSymbolInstance> {
    @Override
    public PSymbolInstance buildOutput(String name) {
        return new EdhocOutputRA(name);
    }
}
