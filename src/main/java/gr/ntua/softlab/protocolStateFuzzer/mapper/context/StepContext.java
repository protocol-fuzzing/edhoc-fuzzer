package gr.ntua.softlab.protocolStateFuzzer.mapper.context;

import gr.ntua.softlab.protocolStateFuzzer.mapper.abstractSymbols.AbstractInput;

public class StepContext {
	protected final int index;
	protected AbstractInput input;

	/**
	 * A boolean for disabling current execution.
	 */
	protected boolean disabled;

	public StepContext(int index) {
		disabled = false;
		this.index = index;
		this.input = null;
	}

	public AbstractInput getInput() {
		return input;
	}

	public void setInput(AbstractInput input) {
		this.input = input;
	}
	
	public boolean isDisabled() {
		return disabled;
	}

	public void disable() {
		disabled = true;
	}
	
	public int getIndex() {
		return index;
	}
}
