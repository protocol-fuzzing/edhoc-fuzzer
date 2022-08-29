package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.CoapExchangeInfo;
import gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.CoapExchanger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.net.InetSocketAddress;

public class EdhocServer extends CoapServer {

    public EdhocServer(String host, int port, String edhocResource, String appGetResource,
                       EdhocStackFactoryPersistent edhocStackFactoryPersistent,
                       CoapExchanger coapExchanger) {

        // add edhocResource
        addLeafResource(createInnerResourceTree(edhocResource),
                new EdhocResource(extractLeafResourceString(edhocResource), coapExchanger));

        // add appGetResource
        addLeafResource(createInnerResourceTree(appGetResource),
                new ApplicationGetResource(extractLeafResourceString(appGetResource), coapExchanger));

        // add endpoint
        CoapEndpoint coapEndpoint = CoapEndpoint.builder()
                .setInetSocketAddress(new InetSocketAddress(host, port))
                .setCoapStackFactory(edhocStackFactoryPersistent)
                .build();

        addEndpoint(coapEndpoint);
    }

    protected String extractLeafResourceString(String resource) {
        String[] resources = resource.split("/");
        return resources[resources.length - 1];
    }

    protected void addLeafResource(CoapResource lastResource, CoapResource leafResource) {
        if (lastResource == null) {
            // leaf resource is also root, so add to server
            this.add(leafResource);
        } else {
            lastResource.add(leafResource);
        }
    }

    protected CoapResource createInnerResourceTree(String resource) {
        // resources list includes names of:
        // root resource, inner resources and leaf resource
        String[] resources = resource.split("/");

        if (resources.length <= 1) {
            // only leaf resource name is present
            return null;
        }

        // Create Root resource and add it to server
        CustomResource rootResource = new CustomResource(resources[0]);
        this.add(rootResource);

        // Last resource before leaf
        CustomResource lastResource;

        if (resources.length == 2) {
            // only root resource and leaf resource are present
            lastResource = rootResource;
        } else {
            // Inner resources without the root and the leaf resource
            CustomResource[] innerResources = new CustomResource[resources.length - 2];

            for (int i = 0; i < innerResources.length; i++) {
                innerResources[i] = new CustomResource(resources[i + 1]);
            }

            // Add resource tree to root resource
            rootResource.add(innerResources);

            // last inner resource is last
            lastResource = innerResources[innerResources.length - 1];
        }

        // return the last resource, in order to add a leaf resource to
        return lastResource;
    }

    // Custom resource (non-leaf in resource tree)
    protected static class CustomResource extends CoapResource {

        public CustomResource(String name) {

            // set resource identifier
            super(name);

            // set display name
            getAttributes().setTitle(name);

        }

        @Override
        public void handleGET(CoapExchange exchange) {

            // respond to the request
            exchange.respond(getName());
        }
    }

    // The resource for edhoc protocol requests
    protected static class EdhocResource extends CoapResource {
        private static final Logger LOGGER = LogManager.getLogger(EdhocResource.class);
        protected CoapExchanger coapExchanger;

        public EdhocResource(String name, CoapExchanger coapExchanger) {
            // set resource identifier
            super(name);

            // set display name
            getAttributes().setTitle(name + " - EDHOC Resource");

            this.coapExchanger = coapExchanger;
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            LOGGER.debug("Received GET request");
            // respond to the request
            exchange.respond("EDHOC protocol expects POST requests");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            LOGGER.debug("Received POST request");

            if (coapExchanger == null) {
                // respond to the request
                exchange.respond("EDHOC POST response");
            } else {
                // edit exchange in draft queue
                CoapExchangeInfo coapExchangeInfo = coapExchanger.getDraftQueue().poll();

                if (coapExchangeInfo == null) {
                    LOGGER.warn("Empty draft queue found");
                    coapExchangeInfo = new CoapExchangeInfo();
                }

                coapExchangeInfo.setCoapExchange(exchange);
                coapExchangeInfo.setHasEdhocMessage(true);

                // save exchange to receivedQueue in order for some observer to respond
                boolean ok = coapExchanger.getReceivedQueue().offer(coapExchangeInfo);

                if (!ok) {
                    LOGGER.warn("Full receivedQueue found");
                }
            }
        }
    }

    // The Resource for application data (oscore-protected) GET requests
    protected static class ApplicationGetResource extends CoapResource {
        private static final Logger LOGGER = LogManager.getLogger(ApplicationGetResource.class);
        protected CoapExchanger coapExchanger;

        public ApplicationGetResource(String name, CoapExchanger coapExchanger) {
            // set resource identifier
            super(name);

            // set display name
            getAttributes().setTitle(name + " - Application Resource");

            this.coapExchanger = coapExchanger;
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            LOGGER.debug("Received GET request");

            if (coapExchanger == null) {
                // respond to the request
                exchange.respond("Application GET response");
            } else {
                // edit exchange in draft queue
                CoapExchangeInfo coapExchangeInfo = coapExchanger.getDraftQueue().poll();

                if (coapExchangeInfo == null) {
                    LOGGER.warn("Empty draft queue found");
                    coapExchangeInfo = new CoapExchangeInfo();
                }

                coapExchangeInfo.setCoapExchange(exchange);
                coapExchangeInfo.setHasApplicationData(true);

                // save exchange to receivedQueue in order for some observer to respond
                boolean ok = coapExchanger.getReceivedQueue().offer(coapExchangeInfo);

                if (!ok) {
                    LOGGER.warn("Full receivedQueue found");
                }
            }
        }
    }
}
