package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.common;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.MessageProcessorPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.PayloadType;

public class CoapAppMessage extends AppMessage {

    public CoapAppMessage(MessageProcessorPersistent messageProcessorPersistent) {
        // initialize fields as ApplicationData
        super(messageProcessorPersistent);
        // non oscore-protected application message
        payloadType = PayloadType.COAP_APP_MESSAGE;
    }
}
