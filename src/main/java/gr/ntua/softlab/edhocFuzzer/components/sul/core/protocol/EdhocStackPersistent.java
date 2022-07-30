package gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol;

import org.eclipse.californium.core.network.Outbox;
import org.eclipse.californium.core.network.stack.*;
import org.eclipse.californium.edhoc.EdhocEndpointInfo;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.oscore.ObjectSecurityContextLayer;
import org.eclipse.californium.oscore.ObjectSecurityLayer;

/** Adapted from {@link org.eclipse.californium.edhoc.EdhocStack} */
public class EdhocStackPersistent extends BaseCoapStack {

    public EdhocStackPersistent(String tag, final Configuration config, final Outbox outbox,
                                EndpointContextMatcher matchingStrategy,
                                EdhocEndpointInfo edhocEndpointInfo,
                                MessageProcessorPersistent messageProcessorPersistent) {
        super(outbox);

        Layer[] layers = new Layer[] {
                new ObjectSecurityContextLayer(edhocEndpointInfo.getOscoreDb()),
                new ExchangeCleanupLayer(config),
                new ObserveLayer(config),
                new BlockwiseLayer(tag, false, config, matchingStrategy),
                CongestionControlLayer.newImplementation(tag, config),
                new ObjectSecurityLayer(edhocEndpointInfo.getOscoreDb()),
                new EdhocLayerPersistent(edhocEndpointInfo, messageProcessorPersistent)
        };

        setLayers(layers);
    }

}
