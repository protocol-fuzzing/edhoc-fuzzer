package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutputChecker;

import java.util.Objects;

public class EdhocOutputChecker implements AbstractOutputChecker {

    public boolean isMessage(AbstractOutput abstractOutput, EdhocOutputType edhocOutputType) {
        return Objects.equals(abstractOutput.getName(), edhocOutputType.name());
    }

    @Override
    public boolean hasInitialClientMessage(AbstractOutput abstractOutput) {
        return isMessage(abstractOutput, EdhocOutputType.EDHOC_MESSAGE_1);
    }
}
