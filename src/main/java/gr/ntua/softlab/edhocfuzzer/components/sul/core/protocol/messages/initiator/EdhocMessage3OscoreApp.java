package gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.initiator;

import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.PayloadType;
import gr.ntua.softlab.edhocfuzzer.components.sul.core.protocol.messages.common.AppMessage;

public class EdhocMessage3OscoreApp extends AppMessage {

    public EdhocMessage3OscoreApp(MessageProcessorPersistent messageProcessorPersistent) {
        // initialize fields as ApplicationData
        super(messageProcessorPersistent);
        // oscore-protected application message combined with edhoc message 3
        payloadType = PayloadType.EDHOC_MESSAGE_3_OSCORE_APP;
    }
}
