package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.messages;

public class Message3Combined extends ApplicationDataMessage {
    public Message3Combined() {
        // initialize fields as ApplicationData
        super();
        // oscore-protected application message combined with edhoc
        // message 3
        payloadType = PayloadType.MESSAGE_3_COMBINED;
    }
}
