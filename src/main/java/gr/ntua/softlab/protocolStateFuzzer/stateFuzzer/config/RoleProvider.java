package gr.ntua.softlab.protocolStateFuzzer.stateFuzzer.config;

public interface RoleProvider {
	/**
	 * @return true if analysis concerns a client implementation, false otherwise
	 */
	boolean isClient();
}
