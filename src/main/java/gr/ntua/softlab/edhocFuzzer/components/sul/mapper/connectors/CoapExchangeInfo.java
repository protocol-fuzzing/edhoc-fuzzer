package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import org.eclipse.californium.core.server.resources.CoapExchange;

public class CoapExchangeInfo {
    protected CoapExchange coapExchange;

    protected int MID;

    protected boolean hasEdhocMessage;

    protected boolean hasProtectedMessage;

    protected boolean hasUnprotectedMessage;

    protected boolean hasUnsuccessfulMessage;

    public CoapExchangeInfo(int MID) {
        this.MID = MID;
        coapExchange = null;
        hasEdhocMessage = false;
        hasProtectedMessage = false;
        hasUnprotectedMessage = false;
        hasUnsuccessfulMessage = false;
    }

    public CoapExchange getCoapExchange() {
        return coapExchange;
    }

    public void setCoapExchange(CoapExchange coapExchange) {
        this.coapExchange = coapExchange;
    }

    public int getMID() {
        return MID;
    }

    public boolean hasEdhocMessage() {
        return hasEdhocMessage;
    }

    public void setHasEdhocMessage(boolean hasEdhocMessage) {
        this.hasEdhocMessage = hasEdhocMessage;
    }

    public boolean hasMsg3CombinedWithAppMessage() {
        return hasEdhocMessage && hasProtectedMessage;
    }

    public boolean hasProtectedMessage() {
        return hasProtectedMessage;
    }

    public void setHasProtectedMessage(boolean hasProtectedMessage) {
        this.hasProtectedMessage = hasProtectedMessage;
    }

    public boolean hasUnprotectedMessage() {
        return hasUnprotectedMessage;
    }

    public void setHasUnprotectedMessage(boolean hasUnprotectedMessage) {
        this.hasUnprotectedMessage = hasUnprotectedMessage;
    }

    public boolean hasUnsuccessfulMessage() {
        return hasUnsuccessfulMessage;
    }

    public void setHasUnsuccessfulMessage(boolean hasUnsuccessfulMessage) {
        this.hasUnsuccessfulMessage = hasUnsuccessfulMessage;
    }
}
