package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;

import java.util.Objects;

public class EdhocOutputCheckerRA implements OutputChecker<EdhocOutputRA> {

    public boolean isMessage(EdhocOutputRA output, MessageOutputType messageOutputType) {
        return Objects.equals(output.getName(), messageOutputType.name());
    }

    @Override
    public boolean hasInitialClientMessage(EdhocOutputRA output) {
        return isMessage(output, MessageOutputType.EDHOC_MESSAGE_1);
    }

    @Override
    public boolean isTimeout(EdhocOutputRA output) {
        return isMessage(output, MessageOutputType.TIMEOUT);
    }

    @Override
    public boolean isUnknown(EdhocOutputRA output) {
        return isMessage(output, MessageOutputType.UNKNOWN);
    }

    @Override
    public boolean isSocketClosed(EdhocOutputRA output) {
        return isMessage(output, MessageOutputType.SOCKET_CLOSED);
    }

    @Override
    public boolean isDisabled(EdhocOutputRA output) {
        return isMessage(output, MessageOutputType.DISABLED);
    }
}
