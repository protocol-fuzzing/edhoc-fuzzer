package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.context;

public interface Authenticator {
    void setupOwnAuthenticationCredentials();

    void setupPeerAuthenticationCredentials();
}
