package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient;

import org.eclipse.californium.core.server.resources.CoapExchange;

public class CoapExchangeWrapper {
    protected CoapExchange coapExchange;

    protected boolean hasEdhocMessage;
    protected boolean hasApplicationData;

    public CoapExchangeWrapper() {
        this.coapExchange = null;
    }

    public CoapExchangeWrapper(CoapExchangeWrapper coapExchangeWrapper) {
        this.coapExchange = coapExchangeWrapper.getCoapExchange();
    }

    public synchronized CoapExchange getCoapExchange() {
        return coapExchange;
    }

    public synchronized void setCoapExchange(CoapExchange coapExchange) {
        this.coapExchange = coapExchange;
        this.notifyAll();
    }

    public boolean hasEdhocMessage() {
        return hasEdhocMessage;
    }

    public void setHasEdhocMessage(boolean hasEdhocMessage) {
        this.hasEdhocMessage = hasEdhocMessage;
    }

    public boolean hasApplicationData() {
        return hasApplicationData;
    }

    public void setHasApplicationData(boolean hasApplicationData) {
        this.hasApplicationData = hasApplicationData;
    }
}
