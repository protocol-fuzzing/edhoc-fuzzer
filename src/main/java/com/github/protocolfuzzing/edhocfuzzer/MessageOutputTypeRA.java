package com.github.protocolfuzzing.edhocfuzzer;

/**
 * Messages that can be received.
 * The last four are from the OutputBuilder.
 */
public enum MessageOutputTypeRA {
    EDHOC_MESSAGE_1_OUTPUT,
    EDHOC_MESSAGE_2_OUTPUT,
    EDHOC_MESSAGE_3_OUTPUT,
    EDHOC_MESSAGE_4_OUTPUT,
    EDHOC_ERROR_MESSAGE_OUTPUT,
    EDHOC_MESSAGE_3_OSCORE_APP_OUTPUT,
    OSCORE_APP_MESSAGE_OUTPUT,
    COAP_APP_MESSAGE_OUTPUT,
    COAP_MESSAGE_OUTPUT,
    COAP_ERROR_MESSAGE_OUTPUT,
    COAP_EMPTY_MESSAGE_OUTPUT
    // The PSF specific symbols like timeout now lives in the PSFOutputSymbols
}
