package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.sulWrappers;

import de.learnlib.api.SUL;
import de.learnlib.api.exception.SULException;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractInput;
import gr.ntua.softlab.protocolStateFuzzer.components.learner.abstractSymbols.AbstractOutput;

public class AbstractIsAliveWrapper implements SUL<AbstractInput, AbstractOutput> {

	protected SUL<AbstractInput, AbstractOutput> sut;
	protected boolean isAlive;
	protected AbstractOutput socketClosedOutput;

	public AbstractIsAliveWrapper(SUL<AbstractInput, AbstractOutput> sut, AbstractOutput socketClosedOutput) {
		this.sut = sut;
		this.socketClosedOutput = socketClosedOutput;
	}

	@Override
	public void pre() {
		sut.pre();
		isAlive = true;
	}

	@Override
	public void post() {
		sut.post();
	}

	@Override
	public AbstractOutput step(AbstractInput in) throws SULException {
		if (isAlive) {
			AbstractOutput out = sut.step(in);
			isAlive = out.isAlive();
			return out;
		} else {
			return socketClosedOutput;
		}
	}
}
