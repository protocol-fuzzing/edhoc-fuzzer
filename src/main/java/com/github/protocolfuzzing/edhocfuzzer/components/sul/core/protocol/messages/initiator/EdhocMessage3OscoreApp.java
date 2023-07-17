package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.initiator;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.PayloadType;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common.AppMessage;

public class EdhocMessage3OscoreApp extends AppMessage {

    public EdhocMessage3OscoreApp(MessageProcessorPersistent messageProcessorPersistent) {
        // initialize fields as ApplicationData
        super(messageProcessorPersistent);
        // oscore-protected application message combined with edhoc message 3
        payloadType = PayloadType.EDHOC_MESSAGE_3_OSCORE_APP;
    }
}
