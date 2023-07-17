package gr.ntua.softlab.edhocfuzzer.components.sul.mapper.connectors;

import org.eclipse.californium.core.server.resources.CoapExchange;

public class CoapExchangeInfo {
    protected CoapExchange coapExchange;
    protected int MID;
    protected boolean hasEdhocMessage;
    protected boolean hasOscoreAppMessage;
    protected boolean hasCoapAppMessage;
    protected boolean hasUnsuccessfulMessage;

    public CoapExchangeInfo(int MID) {
        this.MID = MID;
        coapExchange = null;
        hasEdhocMessage = false;
        hasOscoreAppMessage = false;
        hasCoapAppMessage = false;
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

    public boolean hasOscoreAppMessage() {
        return hasOscoreAppMessage;
    }

    public void setHasOscoreAppMessage(boolean hasOscoreAppMessage) {
        this.hasOscoreAppMessage = hasOscoreAppMessage;
    }

    public boolean hasCoapAppMessage() {
        return hasCoapAppMessage;
    }

    public void setHasCoapAppMessage(boolean hasCoapAppMessage) {
        this.hasCoapAppMessage = hasCoapAppMessage;
    }

    public boolean hasUnsuccessfulMessage() {
        return hasUnsuccessfulMessage;
    }

    public void setHasUnsuccessfulMessage(boolean hasUnsuccessfulMessage) {
        this.hasUnsuccessfulMessage = hasUnsuccessfulMessage;
    }
}
