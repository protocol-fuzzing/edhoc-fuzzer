package gr.ntua.softlab.edhocFuzzer.sul;

import gr.ntua.softlab.protocolStateFuzzer.sul.config.MapperToSulConfig;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.MapperToSulConfigException;
import gr.ntua.softlab.protocolStateFuzzer.sul.config.SulServerConfig;

public class EdhocSulServerConfig extends SulServerConfig {
    @Override
    public void applyDelegate(MapperToSulConfig config) throws MapperToSulConfigException {
    }
}
