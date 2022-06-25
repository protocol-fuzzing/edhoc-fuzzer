package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.State;
import org.eclipse.californium.edhoc.EdhocSession;

public abstract class EdhocMapperState extends State {
    public abstract EdhocSession getEdhocSession();

    public abstract void setEdhocSession(EdhocSession edhocSession);
}
