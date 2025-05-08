package com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.symbols.outputs;

/** Messages that can be received.
 * The last four are from the OutputBuilder.
 */
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
    UNSUCCESSFUL_MESSAGE,
    TIMEOUT,
    UNKNOWN,
    SOCKET_CLOSED,
    DISABLED
}
