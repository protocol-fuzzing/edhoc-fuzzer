package io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common;

import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import io.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.PayloadType;

public class OscoreAppMessage extends AppMessage {

    public OscoreAppMessage(MessageProcessorPersistent messageProcessorPersistent) {
        // initialize fields as ApplicationData
        super(messageProcessorPersistent);
        // oscore-protected application message
        payloadType = PayloadType.OSCORE_APP_MESSAGE;
    }
}
