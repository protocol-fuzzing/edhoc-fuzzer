package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config;

import com.beust.jcommander.Parameter;

public abstract class SulServerConfig extends SulConfig {

	@Parameter(names = "-connect", required = true, description = "Address of server to connect the client. Format: ip:port")
	protected String host = null;

	public SulServerConfig() {
		super();
	}

	public abstract void applyDelegate(MapperToSulConfig config) throws MapperToSulConfigException;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public final String getFuzzingRole() {
		return "server";
	}
	
	public final boolean isFuzzingClient() {
		return false;
	}
	
}
