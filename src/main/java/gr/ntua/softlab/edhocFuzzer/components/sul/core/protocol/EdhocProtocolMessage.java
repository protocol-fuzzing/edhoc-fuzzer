package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import gr.ntua.softlab.protocolStateFuzzer.components.sul.core.protocol.ProtocolMessage;

public abstract class EdhocProtocolMessage extends ProtocolMessage {
    protected byte[] cborSequence;

    public byte[] getCBORSequence() {
        return cborSequence;
    }
}
