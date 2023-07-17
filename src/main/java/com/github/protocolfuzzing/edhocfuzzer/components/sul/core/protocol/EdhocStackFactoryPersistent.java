package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.ExtendedCoapStackFactory;
import org.eclipse.californium.core.network.Outbox;
import org.eclipse.californium.core.network.stack.CoapStack;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.config.Configuration;

/** Adapted from {@link org.eclipse.californium.edhoc.EdhocCoapStackFactory} */
public class EdhocStackFactoryPersistent implements ExtendedCoapStackFactory {

    protected EdhocEndpointInfoPersistent edhocEndpointInfoPersistent;
    protected MessageProcessorPersistent messageProcessorPersistent;

    public EdhocStackFactoryPersistent(EdhocEndpointInfoPersistent edhocEndpointInfoPersistent,
                                 MessageProcessorPersistent messageProcessorPersistent) {
        this.edhocEndpointInfoPersistent = edhocEndpointInfoPersistent;
        this.messageProcessorPersistent = messageProcessorPersistent;
    }

    @Override
    public CoapStack createCoapStack(String protocol, String tag, Configuration config,
                                     EndpointContextMatcher matchingStrategy, Outbox outbox,
                                     Object customStackArgument) {
        if (CoAP.isTcpProtocol(protocol)) {
            throw new IllegalArgumentException("protocol is not supported: " + protocol);
        }

        if (customStackArgument != null) {
            throw new IllegalArgumentException("erroneously provided custom coap stack: " + customStackArgument);
        }

        return new EdhocStackPersistent(tag, config, outbox, matchingStrategy,
                edhocEndpointInfoPersistent, messageProcessorPersistent);
    }

    @SuppressWarnings("deprecation")
    @Override
    public CoapStack createCoapStack(String protocol, String tag, Configuration config, Outbox outbox,
                                     Object customStackArgument) {
        throw new UnsupportedOperationException("Deprecated function call");
    }
}
