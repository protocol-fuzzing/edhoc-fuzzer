package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import org.eclipse.californium.core.server.resources.CoapExchange;

public class CoapExchangeInfo {
    protected CoapExchange coapExchange;

    protected boolean hasEdhocMessage;
    protected boolean hasApplicationData;

    public CoapExchangeInfo() {
        coapExchange = null;
        hasEdhocMessage = false;
        hasApplicationData = false;
    }

    public CoapExchange getCoapExchange() {
        return coapExchange;
    }

    public void setCoapExchange(CoapExchange coapExchange) {
        this.coapExchange = coapExchange;
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

    public boolean hasApplicationDataAfterMessage3() {
        return hasEdhocMessage() && hasApplicationData();
    }
}
