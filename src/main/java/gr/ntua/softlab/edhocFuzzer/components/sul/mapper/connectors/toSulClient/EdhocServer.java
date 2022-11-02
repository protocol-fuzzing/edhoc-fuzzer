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

    public EdhocServer(String host, int port, String edhocResource, String appResource,
                       EdhocStackFactoryPersistent edhocStackFactoryPersistent,
                       CoapExchanger coapExchanger) {

        // add edhocResource
        addLeafResource(createInnerResourceTree(edhocResource),
                new EdhocResource(extractLeafResourceString(edhocResource), coapExchanger));

        // add appResource
        addLeafResource(createInnerResourceTree(appResource),
                new ApplicationResource(extractLeafResourceString(appResource), coapExchanger));

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
        public void handlePOST(CoapExchange exchange) {
            // respond to the request
            exchange.respond(getName());
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
                EdhocServer.handleExchange(true, exchange, coapExchanger);
            }
        }
    }

    // The Resource for application data requests
    protected static class ApplicationResource extends CoapResource {
        private static final Logger LOGGER = LogManager.getLogger(ApplicationResource.class);
        protected CoapExchanger coapExchanger;

        public ApplicationResource(String name, CoapExchanger coapExchanger) {
            // set resource identifier
            super(name);

            // set display name
            getAttributes().setTitle(name + " - Application Resource");

            this.coapExchanger = coapExchanger;
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            LOGGER.debug("Received POST request");
            handleExchange(exchange);
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            LOGGER.debug("Received GET request");
            handleExchange(exchange);
        }

        protected void handleExchange(CoapExchange exchange) {
            if (coapExchanger == null) {
                // respond to the request
                exchange.respond("Application response");
            } else {
                EdhocServer.handleExchange(false, exchange, coapExchanger);
            }
        }
    }

    protected static void handleExchange(boolean fromEdhocResource, CoapExchange coapExchange,
                                         CoapExchanger coapExchanger) {
        // edit coapExchange in draft queue
        CoapExchangeInfo coapExchangeInfo;
        int MID = coapExchange.advanced().getRequest().getMID();

        do {
            // if encountered a coapExchangeInfo not intended for the current coapExchange then
            // drop it from draft queue and continue searching
            coapExchangeInfo = coapExchanger.getDraftQueue().poll();
        } while (coapExchangeInfo != null &&  coapExchangeInfo.getMID() != MID);

        if (coapExchangeInfo == null) {
            LOGGER.warn("Empty draft queue found");
            coapExchangeInfo = new CoapExchangeInfo(MID);
        }

        coapExchangeInfo.setCoapExchange(coapExchange);

        if (fromEdhocResource) {
          coapExchangeInfo.setHasEdhocMessage(true);
        } else {
            if (coapExchange.getRequestOptions().hasOscore() &&
                    coapExchange.advanced().getCryptographicContextID() != null) {
                // request was oscore-protected
                coapExchangeInfo.setHasOscoreAppMessage(true);
            } else {
                // request was unprotected
                coapExchangeInfo.setHasCoapAppMessage(true);
            }
        }

        // save coapExchange to receivedQueue in order for some observer to respond
        boolean ok = coapExchanger.getReceivedQueue().offer(coapExchangeInfo);

        if (!ok) {
            LOGGER.warn("Full receivedQueue found");
        }
    }
}
