package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutputChecker;

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
