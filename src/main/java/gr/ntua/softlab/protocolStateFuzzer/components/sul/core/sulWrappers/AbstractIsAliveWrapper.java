package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers;

import de.learnlib.api.SUL;
import de.learnlib.api.exception.SULException;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput;

public class AbstractIsAliveWrapper implements SUL<AbstractInput, AbstractOutput> {

	protected SUL<AbstractInput, AbstractOutput> sul;
	protected boolean isAlive;
	protected AbstractOutput socketClosedOutput;

	public AbstractIsAliveWrapper(SUL<AbstractInput, AbstractOutput> sul, AbstractOutput socketClosedOutput) {
		this.sul = sul;
		this.socketClosedOutput = socketClosedOutput;
	}

	@Override
	public void pre() {
		sul.pre();
		isAlive = true;
	}

	@Override
	public void post() {
		sul.post();
	}

	@Override
	public AbstractOutput step(AbstractInput in) throws SULException {
		if (isAlive) {
			AbstractOutput out = sul.step(in);
			isAlive = out.isAlive();
			return out;
		} else {
			return socketClosedOutput;
		}
	}
}
