package gr.ntua.softlab.protocolStateFuzzer.sul.config;

import com.beust.jcommander.Parameter;

public abstract class SulServerDelegate extends SulDelegate {

	@Parameter(names = "-clientWait", required = false, description = "Time before starting the client")
	protected Long clientWait = 50L;

	@Parameter(names = "-port", required = true, description = "The port on which the server should listen")
	protected Integer port = null;

	public SulServerDelegate() {
		super();
	}

	public abstract void applyDelegate(SulConfig config) throws SulConfigurationException;

	public Long getClientWait() {
		return clientWait;
	}

	public Integer getPort() {
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
