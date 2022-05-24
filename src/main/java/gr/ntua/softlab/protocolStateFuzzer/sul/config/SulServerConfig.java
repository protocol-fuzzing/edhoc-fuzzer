package gr.ntua.softlab.protocolStateFuzzer.sul.config;

import com.beust.jcommander.Parameter;

public abstract class SulServerConfig extends SulConfig {

	@Parameter(names = "-clientWait", required = false, description = "Time before starting the client")
	protected Long clientWait = 50L;

	@Parameter(names = "-port", required = true, description = "The port on which the server should listen")
	protected Integer port = null;

	public SulServerConfig() {
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
	public final String getRole() {
		return "server";
	}
	
	public final boolean isClient() {
		return false;
	}
}
