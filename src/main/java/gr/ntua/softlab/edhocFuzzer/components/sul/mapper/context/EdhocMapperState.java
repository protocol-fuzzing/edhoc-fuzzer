package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.context.State;
import org.eclipse.californium.edhoc.EdhocEndpointInfo;
import org.eclipse.californium.edhoc.EdhocSession;

import java.util.Set;

public abstract class EdhocMapperState extends State {
    public abstract EdhocSession getEdhocSession();

    public abstract EdhocEndpointInfo getEdhocEndpointInfo();

    public abstract Set<CBORObject> getOwnIdCreds();

    public abstract void setupOscoreContext();
}
