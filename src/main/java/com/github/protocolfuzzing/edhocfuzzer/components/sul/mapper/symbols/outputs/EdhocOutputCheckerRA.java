package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.learner.alphabet.PSFOutputSymbols;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;
import de.learnlib.ralib.words.PSymbolInstance;

import java.util.Objects;

public class EdhocOutputCheckerRA implements OutputChecker<PSymbolInstance> {

    public <E extends Enum<E>> boolean isMessage(PSymbolInstance output, E messageOutputType) {
        return Objects.equals(output.getBaseSymbol().getName(), messageOutputType.name());
    }

    @Override
    public boolean hasInitialClientMessage(PSymbolInstance output) {
        return isMessage(output, MessageOutputTypeRA.EDHOC_MESSAGE_1_OUTPUT);
    }

    @Override
    public boolean isTimeout(PSymbolInstance output) {
        return isMessage(output, PSFOutputSymbols.TIMEOUT);
    }

    @Override
    public boolean isUnknown(PSymbolInstance output) {
        return isMessage(output, PSFOutputSymbols.UNKNOWN);
    }

    @Override
    public boolean isSocketClosed(PSymbolInstance output) {
        return isMessage(output, PSFOutputSymbols.SOCKET_CLOSED);
    }

    @Override
    public boolean isDisabled(PSymbolInstance output) {
        return isMessage(output, PSFOutputSymbols.DISABLED);
    }
}
