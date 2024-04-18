package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputBuilder;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;

public class EdhocOutputBuilderRA implements OutputBuilder<PSymbolInstance> {

    @Override
    public PSymbolInstance buildOutput(String name) {
        OutputSymbol baseSymbol = new OutputSymbol(name);
        return new PSymbolInstance(baseSymbol);
    }

    public PSymbolInstance buildOutput(MessageOutputType type) {
        OutputSymbol baseSymbol = new OutputSymbol(type.name());
        return new PSymbolInstance(baseSymbol);
    }

    @Override
    public PSymbolInstance buildTimeout() {
        return buildOutput(MessageOutputType.TIMEOUT);
    }

    @Override
    public PSymbolInstance buildUnknown() {
        return buildOutput(MessageOutputType.UNKNOWN);
    }

    @Override
    public PSymbolInstance buildSocketClosed() {
        return buildOutput(MessageOutputType.SOCKET_CLOSED);
    }

    @Override
    public PSymbolInstance buildDisabled() {
        return buildOutput(MessageOutputType.DISABLED);
    }
}
