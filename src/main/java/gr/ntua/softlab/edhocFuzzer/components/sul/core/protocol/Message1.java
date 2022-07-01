package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

public class Message1 extends EdhocProtocolMessage {

    public Message1(MessageProcessorPersistent messageProcessorPersistent) {
        cborSequence = messageProcessorPersistent.writeMessage1();
    }
}
