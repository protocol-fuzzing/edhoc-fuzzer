package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol.messages.PayloadType;

public interface EdhocMapperConnector {

    void initialize(EdhocStackFactoryPersistent edhocStackFactoryPersistent, CoapExchanger coapExchanger);

    void send(byte[] payload, PayloadType payloadType, int messageCode, int contentFormat);

    byte[] receive() throws GenericErrorException, TimeoutException,
            UnsupportedMessageException, UnsuccessfulMessageException;

    void setTimeout(Long timeout);

    boolean receivedCoapErrorMessage();

    boolean receivedOscoreAppMessage();

    boolean receivedCoapAppMessage();

    boolean receivedMsg3WithOscoreApp();

    boolean receivedCoapEmptyMessage();
}
