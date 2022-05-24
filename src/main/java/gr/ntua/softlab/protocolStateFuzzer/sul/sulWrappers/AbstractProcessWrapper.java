package gr.ntua.softlab.protocolStateFuzzer.sul.sulWrappers;

import de.learnlib.api.SUL;
import de.learnlib.api.exception.SULException;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulConfig;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractOutput;

public class AbstractProcessWrapper extends SulProcessWrapper<AbstractInput, AbstractOutput> {

    public AbstractProcessWrapper(SUL<AbstractInput, AbstractOutput> sul, SulConfig sulConfig) {
        super(sul, sulConfig);
    }

    @Override
    public AbstractOutput step(AbstractInput in) throws SULException {
        AbstractOutput output = super.step(in);
        output.setAlive(super.isAlive());
        return output;
    }
}
