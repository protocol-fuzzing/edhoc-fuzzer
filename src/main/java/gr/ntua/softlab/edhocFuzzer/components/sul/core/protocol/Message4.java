package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

public class Message4 extends EdhocProtocolMessage {

    public Message4(MessageProcessorPersistent messageProcessorPersistent) {
        cborSequence = messageProcessorPersistent.writeMessage4();
    }
}
