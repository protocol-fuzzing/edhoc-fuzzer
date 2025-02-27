package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;

public class ServerMapperState extends EdhocMapperState {

    public ServerMapperState(EdhocMapperConfig edhocMapperConfig, CleanupTasks cleanupTasks) {
        super(edhocMapperConfig, edhocMapperConfig.getEdhocCoapUri(), edhocMapperConfig.getHostCoapUri(), cleanupTasks);
    }

    @Override
    public boolean isCoapClient() {
        return false;
    }
}
