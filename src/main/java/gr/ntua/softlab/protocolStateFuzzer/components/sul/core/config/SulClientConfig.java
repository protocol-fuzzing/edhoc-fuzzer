package gr.ntua.softlab.protocolStateFuzzer.components.sul.core.config;

import com.beust.jcommander.Parameter;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfig;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.config.MapperConnectionConfigException;

public abstract class SulClientConfig extends SulConfig {

	@Parameter(names = "-clientWait", description = "Time (ms) before starting the client")
	protected Long clientWait = 50L;

	@Parameter(names = "-port", required = true, description = "The port on which the server should listen")
	protected Integer port = null;

	public SulClientConfig() {
		super();
	}

	public SulClientConfig(MapperConfig mapperConfig) {
		super(mapperConfig);
	}

	public abstract void applyDelegate(MapperConnectionConfig config) throws MapperConnectionConfigException;

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
