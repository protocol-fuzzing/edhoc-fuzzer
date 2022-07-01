package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

public class Message2 extends EdhocProtocolMessage {

    public Message2(MessageProcessorPersistent messageProcessorPersistent) {
        cborSequence = messageProcessorPersistent.writeMessage2();
    }
}
