package gr.ntua.softlab.protocolStateFuzzer.sul.config;

import com.beust.jcommander.Parameter;

public abstract class SulClientDelegate extends SulDelegate {

	@Parameter(names = "-connect", required = true, description = "Address of server to connect the client. Format: ip:port")
	protected String host = null;

	public SulClientDelegate() {
		super();
	}

	public abstract void applyDelegate(SulConfig config) throws SulConfigurationException;

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
