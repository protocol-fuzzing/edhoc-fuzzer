package gr.ntua.softlab.edhocFuzzer.components.sul.mapper.connectors;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CoapExchanger {
    protected BlockingQueue<CoapExchangeInfo> receivedQueue;
    protected BlockingQueue<CoapExchangeInfo> draftQueue;

    public CoapExchanger() {
        receivedQueue = new ArrayBlockingQueue<>(1);
        draftQueue = new ArrayBlockingQueue<>(1);
    }

    public BlockingQueue<CoapExchangeInfo> getReceivedQueue() {
        return receivedQueue;
    }

    public BlockingQueue<CoapExchangeInfo> getDraftQueue() {
        return draftQueue;
    }
}
