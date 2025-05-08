package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.OutputChecker;

import java.util.Objects;

public class EdhocOutputChecker implements OutputChecker<EdhocOutput> {

    public boolean isMessage(EdhocOutput output, MessageOutputType messageOutputType) {
        return Objects.equals(output.getName(), messageOutputType.name());
    }

    @Override
    public boolean hasInitialClientMessage(EdhocOutput output) {
        return isMessage(output, MessageOutputType.EDHOC_MESSAGE_1);
    }

    @Override
    public boolean isTimeout(EdhocOutput output) {
        return isMessage(output, MessageOutputType.TIMEOUT);
    }

    @Override
    public boolean isUnknown(EdhocOutput output) {
        return isMessage(output, MessageOutputType.UNKNOWN);
    }

    @Override
    public boolean isSocketClosed(EdhocOutput output) {
        return isMessage(output, MessageOutputType.SOCKET_CLOSED);
    }

    @Override
    public boolean isDisabled(EdhocOutput output) {
        return isMessage(output, MessageOutputType.DISABLED);
    }
}
