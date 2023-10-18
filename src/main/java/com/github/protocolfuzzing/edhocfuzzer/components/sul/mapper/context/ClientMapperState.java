package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.context;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.EdhocMapperConfig;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.ProtocolVersion;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;

public class ClientMapperState extends EdhocMapperState {

    public ClientMapperState(ProtocolVersion protocolVersion,  EdhocMapperConfig edhocMapperConfig,
                             CleanupTasks cleanupTasks) {
        super(protocolVersion, edhocMapperConfig, edhocMapperConfig.getEdhocCoapUri(),
              edhocMapperConfig.getEdhocCoapUri(), cleanupTasks);
    }

    @Override
    public boolean isCoapClient() {
        return true;
    }
}
