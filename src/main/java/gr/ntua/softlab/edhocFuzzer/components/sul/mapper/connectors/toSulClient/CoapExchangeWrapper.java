package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors.toSulClient;

import org.eclipse.californium.core.server.resources.CoapExchange;

public class CoapExchangeWrapper {
    protected CoapExchange coapExchange;

    protected boolean hasEdhocMessage;
    protected boolean hasApplicationData;

    public CoapExchangeWrapper() {
        this.coapExchange = null;
    }

    public void reset() {
        this.coapExchange = null;
        this.hasEdhocMessage = false;
        this.hasApplicationData = false;
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

    public boolean hasApplicationDataAfterMessage3() {
        return hasEdhocMessage() && hasApplicationData();
    }

    public void setHasApplicationData(boolean hasApplicationData) {
        this.hasApplicationData = hasApplicationData;
    }
}
