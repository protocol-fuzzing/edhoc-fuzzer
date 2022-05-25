package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers;

import de.learnlib.api.SUL;
import de.learnlib.api.exception.SULException;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractOutput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config.SulConfig;

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
