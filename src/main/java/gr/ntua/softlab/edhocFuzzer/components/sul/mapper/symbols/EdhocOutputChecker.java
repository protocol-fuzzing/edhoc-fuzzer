package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutputChecker;

public class EdhocOutputChecker implements AbstractOutputChecker {
    @Override
    public boolean hasInitialClientMessage(AbstractOutput abstractOutput) {
        return false;
    }
}
