package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient;

import gr.ntua.softlab.edhocFuzzer.components.sul.core.protocol.EdhocStackFactoryPersistent;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.net.InetSocketAddress;

public class EdhocServer extends CoapServer {

    public EdhocServer(String host, int port, String edhocResource, String appGetResource,
                       EdhocStackFactoryPersistent edhocStackFactoryPersistent,
                       CoapExchangeWrapper coapExchangeWrapper) {

        // add edhocResource
        addLeafResource(createInnerResourceTree(edhocResource),
                new EdhocResource(extractLeafResourceString(edhocResource), coapExchangeWrapper));

        // add appGetResource
        addLeafResource(createInnerResourceTree(appGetResource),
                new ApplicationGetResource(extractLeafResourceString(appGetResource), coapExchangeWrapper));

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
        protected CoapExchangeWrapper sharedCoapExhangeWrapper;

        public EdhocResource(String name, CoapExchangeWrapper coapExchangeWrapper) {
            // set resource identifier
            super(name);

            // set display name
            getAttributes().setTitle(name + " - EDHOC Resource");

            this.sharedCoapExhangeWrapper = coapExchangeWrapper;
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            // respond to the request
            exchange.respond("EDHOC protocol expects POST requests");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            if (sharedCoapExhangeWrapper == null) {
                // respond to the request
                exchange.respond("EDHOC POST response");
            } else {
                // save exchange to sharedCoapExchangeWrapper
                // in order some observer of this wrapper to respond
                sharedCoapExhangeWrapper.setCoapExchange(exchange);
                sharedCoapExhangeWrapper.setHasEdhocMessage(true);
                sharedCoapExhangeWrapper.setHasApplicationData(false);
            }
        }
    }

    // The Resource for application data (oscore-protected) GET requests
    protected static class ApplicationGetResource extends CoapResource {
        protected CoapExchangeWrapper sharedCoapExhangeWrapper;

        public ApplicationGetResource(String name, CoapExchangeWrapper coapExchangeWrapper) {
            // set resource identifier
            super(name);

            // set display name
            getAttributes().setTitle(name + " - Application Resource");

            this.sharedCoapExhangeWrapper = coapExchangeWrapper;
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            if (sharedCoapExhangeWrapper == null) {
                // respond to the request
                exchange.respond("Application GET response");
            } else {
                // save exchange to sharedCoapExchangeWrapper
                // in order some observer of this wrapper to respond
                sharedCoapExhangeWrapper.setCoapExchange(exchange);
                sharedCoapExhangeWrapper.setHasEdhocMessage(false);
                sharedCoapExhangeWrapper.setHasApplicationData(true);
            }
        }
    }
}
