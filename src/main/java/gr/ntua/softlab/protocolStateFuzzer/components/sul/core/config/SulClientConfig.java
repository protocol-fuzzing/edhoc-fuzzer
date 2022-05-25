package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config;

import com.beust.jcommander.Parameter;

public abstract class SulClientConfig extends SulConfig {

	@Parameter(names = "-clientWait", description = "Time (ms) before starting the client")
	protected Long clientWait = 50L;

	@Parameter(names = "-port", required = true, description = "The port on which the server should listen")
	protected Integer port = null;

	public SulClientConfig() {
		super();
	}

	public abstract void applyDelegate(MapperToSulConfig config) throws MapperToSulConfigException;

	public long getClientWait() {
		return clientWait;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public final String getFuzzingRole() {
		return "client";
	}
	
	public final boolean isFuzzingClient() {
		return true;
	}
}
