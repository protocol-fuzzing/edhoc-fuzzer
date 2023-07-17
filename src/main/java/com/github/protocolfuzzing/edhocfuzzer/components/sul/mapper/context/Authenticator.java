package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

public interface Authenticator {
    void setupOwnAuthenticationCredentials();

    void setupPeerAuthenticationCredentials();
}
