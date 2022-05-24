package gr.ntua.softlab.protocolStateFuzzer.sul.config;

import com.beust.jcommander.Parameter;

public abstract class SulClientConfig extends SulConfig {

	@Parameter(names = "-connect", required = true, description = "Address of server to connect the client. Format: ip:port")
	protected String host = null;

	public SulClientConfig() {
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
	public final String getRole() {
		return "client";
	}
	
	public final boolean isClient() {
		return true;
	}
	
}
