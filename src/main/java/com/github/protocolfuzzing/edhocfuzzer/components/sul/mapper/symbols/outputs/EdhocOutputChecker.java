package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractOutput;
import com.github.protocolfuzzing.protocolstatefuzzer.components.sul.mapper.abstractsymbols.AbstractOutputChecker;

import java.util.Objects;

public class EdhocOutputChecker implements AbstractOutputChecker {

    public boolean isMessage(AbstractOutput abstractOutput, MessageOutputType messageOutputType) {
        return Objects.equals(abstractOutput.getName(), messageOutputType.name());
    }

    @Override
    public boolean hasInitialClientMessage(AbstractOutput abstractOutput) {
        return isMessage(abstractOutput, MessageOutputType.EDHOC_MESSAGE_1);
    }
}
