package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.State;
import org.eclipse.californium.edhoc.EdhocSession;

public class EdhocState extends State {
    protected EdhocSession edhocSession;

    public EdhocState (EdhocSession edhocSession) {
        this.edhocSession = edhocSession;
    }

    public EdhocSession getEdhocSession() {
        return edhocSession;
    }

    public void setEdhocSession(EdhocSession edhocSession) {
        this.edhocSession = edhocSession;
    }
}
