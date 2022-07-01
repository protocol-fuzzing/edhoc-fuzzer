package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.context;

import com.upokecenter.cbor.CBORObject;
import org.eclipse.californium.edhoc.EdhocEndpointInfo;
import org.eclipse.californium.edhoc.EdhocSession;

import java.util.Set;

public class ServerMapperState extends EdhocMapperState {
    // TODO fill

    @Override
    public EdhocSession getEdhocSession() {
        return null;
    }

    @Override
    public void setEdhocSession(EdhocSession edhocSession) {

    }

    @Override
    public EdhocEndpointInfo getEdhocEndpointInfo() {
        return null;
    }

    @Override
    public Set<CBORObject> getOwnIdCreds() {
        return null;
    }
}
