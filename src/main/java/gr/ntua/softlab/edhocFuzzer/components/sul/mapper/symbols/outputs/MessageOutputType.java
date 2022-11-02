package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.symbols.outputs;

/** Messages that can be received.
 *  Possible additional messages not included are those generated from
 *  {@link gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput#unknown() AbstractOutput.unknown()},
 *  {@link gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput#socketClosed() AbstractOutput.socketClosed()},
 *  {@link gr.ntua.softlab.protocolStateFuzzer.components.sul.mapper.abstractSymbols.AbstractOutput#timeout() AbstractOutput.timeout()} */
public enum MessageOutputType {
    EDHOC_MESSAGE_1,
    EDHOC_MESSAGE_2,
    EDHOC_MESSAGE_3,
    EDHOC_MESSAGE_4,
    EDHOC_ERROR_MESSAGE,
    EDHOC_MESSAGE_3_OSCORE_APP,
    OSCORE_APP_MESSAGE,
    COAP_APP_MESSAGE,
    COAP_MESSAGE,
    COAP_ERROR_MESSAGE,
    COAP_EMPTY_MESSAGE,
    UNSUPPORTED_MESSAGE,
    UNSUCCESSFUL_MESSAGE
}
