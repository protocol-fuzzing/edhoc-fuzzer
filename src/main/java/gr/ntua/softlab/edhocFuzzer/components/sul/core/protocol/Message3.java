package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

public class Message3 extends EdhocProtocolMessage {

    public Message3(MessageProcessorPersistent messageProcessorPersistent) {
        cborSequence = messageProcessorPersistent.writeMessage3();
    }
}
