package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

public interface Authenticator {
    void setupOwnAuthenticationCredentials();

    void setupPeerAuthenticationCredentials();
}
