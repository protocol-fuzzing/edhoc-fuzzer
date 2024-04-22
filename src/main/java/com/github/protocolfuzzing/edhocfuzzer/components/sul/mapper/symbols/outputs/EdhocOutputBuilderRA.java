package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.edhocfuzzer.PSFOutputSymbols;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputBuilder;
import de.learnlib.ralib.words.OutputSymbol;
import de.learnlib.ralib.words.PSymbolInstance;

public class EdhocOutputBuilderRA implements OutputBuilder<PSymbolInstance> {

    @Override
    public PSymbolInstance buildOutput(String name) {
        OutputSymbol baseSymbol = new OutputSymbol(name);
        return new PSymbolInstance(baseSymbol);
    }

    public PSymbolInstance buildOutput(PSFOutputSymbols type) {
        OutputSymbol baseSymbol = new OutputSymbol(type.name());
        return new PSymbolInstance(baseSymbol);
    }

    public PSymbolInstance buildUnsupportedMessage() {
        return buildOutput(PSFOutputSymbols.UNSUPPORTED_MESSAGE);
    }

    public PSymbolInstance buildUnsuccessfulMessage() {
        return buildOutput(PSFOutputSymbols.UNSUCCESSFUL_MESSAGE);
    }

    @Override
    public PSymbolInstance buildTimeout() {
        return buildOutput(PSFOutputSymbols.TIMEOUT);
    }

    @Override
    public PSymbolInstance buildUnknown() {
        return buildOutput(PSFOutputSymbols.UNKNOWN);
    }

    @Override
    public PSymbolInstance buildSocketClosed() {
        return buildOutput(PSFOutputSymbols.SOCKET_CLOSED);
    }

    @Override
    public PSymbolInstance buildDisabled() {
        return buildOutput(PSFOutputSymbols.DISABLED);
    }
}
