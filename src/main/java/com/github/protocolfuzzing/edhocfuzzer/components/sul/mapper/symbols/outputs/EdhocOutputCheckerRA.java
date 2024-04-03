package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;
import de.learnlib.ralib.words.PSymbolInstance;

import java.util.Objects;

public class EdhocOutputCheckerRA implements OutputChecker<PSymbolInstance> {

    public boolean isMessage(PSymbolInstance output, MessageOutputType messageOutputType) {
        return Objects.equals(output.getBaseSymbol().getName(), messageOutputType.name());
    }

    @Override
    public boolean hasInitialClientMessage(PSymbolInstance output) {
        return isMessage(output, MessageOutputType.EDHOC_MESSAGE_1);
    }

    @Override
    public boolean isTimeout(PSymbolInstance output) {
        return isMessage(output, MessageOutputType.TIMEOUT);
    }

    @Override
    public boolean isUnknown(PSymbolInstance output) {
        return isMessage(output, MessageOutputType.UNKNOWN);
    }

    @Override
    public boolean isSocketClosed(PSymbolInstance output) {
        return isMessage(output, MessageOutputType.SOCKET_CLOSED);
    }

    @Override
    public boolean isDisabled(PSymbolInstance output) {
        return isMessage(output, MessageOutputType.DISABLED);
    }
}
