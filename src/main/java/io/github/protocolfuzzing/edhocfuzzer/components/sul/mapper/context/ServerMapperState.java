package io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import io.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;

public class ServerMapperState extends EdhocMapperState {

    public ServerMapperState(EdhocMapperConfig edhocMapperConfig, CleanupTasks cleanupTasks) {
        super(edhocMapperConfig, edhocMapperConfig.getEdhocCoapUri(), edhocMapperConfig.getHostCoapUri(), cleanupTasks);
    }

    @Override
    public boolean isCoapClient() {
        return false;
    }
}
