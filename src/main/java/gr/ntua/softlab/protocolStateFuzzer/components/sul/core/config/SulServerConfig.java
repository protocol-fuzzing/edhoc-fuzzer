package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config;

import com.beust.jcommander.Parameter;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;

public abstract class SulServerConfig extends SulConfig {

	@Parameter(names = "-connect", required = true, description = "Address of server to connect the client. Format: ip:port")
	protected String host = null;

	public SulServerConfig(MapperConfig mapperConfig) {
		super(mapperConfig);
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
