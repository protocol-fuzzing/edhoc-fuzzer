package gr.ntua.softlab.protocolStateFuzzer.learner.statistics;

public class StatisticsSnapshot {
	public long getResets() {
		return resets;
	}

	public long getInputs() {
		return inputs;
	}

	public long getTime() {
		return time;
	}

	protected long resets;
	protected long inputs;
	protected long time;
	
	public StatisticsSnapshot(long resets, long inputs, long time) {
		super();
		this.resets = resets;
		this.inputs = inputs;
		this.time = time;
	}
}
