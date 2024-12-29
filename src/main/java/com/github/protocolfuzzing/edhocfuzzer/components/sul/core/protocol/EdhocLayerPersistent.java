package com.github.protocolfuzzing.edhocfuzzer.components.sul.core.protocol;

import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.config.CombinedMessageVersion;
import com.github.protocolfuzzing.edhocfuzzer.components.sul.mapper.connectors.CoapExchangeInfo;
import com.github.protocolfuzzing.protocolstatefuzzer.utils.CleanupTasks;
import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Message;
import org.eclipse.californium.core.coap.MessageObserverAdapter;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.stack.AbstractLayer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.edhoc.Util;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSCoreCtxDB;
import org.eclipse.californium.oscore.OSException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Adapted from {@link org.eclipse.californium.edhoc.EdhocLayer} */
public class EdhocLayerPersistent extends AbstractLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    protected Concretizer sendConcretizer = null;
    protected Concretizer recvConcretizer = null;

    // The OSCORE context database
    OSCoreCtxDB ctxDb;

    // Map of existing EDHOC sessions
    Map<CBORObject, EdhocSessionPersistent> edhocSessionsPersistent;

    // MessageProcessor for reading message 3
    MessageProcessorPersistent messageProcessorPersistent;

    public EdhocLayerPersistent(EdhocEndpointInfoPersistent edhocEndpointInfoPersistent,
                                MessageProcessorPersistent messageProcessorPersistent) {
        LOGGER.debug("Initializing EDHOC layer persistent");

        this.ctxDb = edhocEndpointInfoPersistent.getOscoreDb();
        this.edhocSessionsPersistent = edhocEndpointInfoPersistent.getEdhocSessionsPersistent();
        this.messageProcessorPersistent = messageProcessorPersistent;

        CleanupTasks cleanupTasks = messageProcessorPersistent.getEdhocMapperState().getCleanupTasks();
        String path = messageProcessorPersistent.getEdhocMapperState().getEdhocMapperConfig().getConcretizeDir();
        if (path != null) {
            this.sendConcretizer = new Concretizer(path, "send");
            this.recvConcretizer = new Concretizer(path, "recv");
            cleanupTasks.submit(sendConcretizer::close);
            cleanupTasks.submit(recvConcretizer::close);
        }
    }

    @Override
    public void sendRequest(final Exchange exchange, final Request request) {

        LOGGER.debug("Sending request through EDHOC layer");

        if (request.getOptions().hasEdhoc() && request.getOptions().hasOscore()) {
            LOGGER.debug("Combined EDHOC+OSCORE request");

            // Retrieve the Security Context used to protect the request
            OSCoreCtx ctx = getContextForOutgoing(exchange);

            // The connection identifier of this peer is its Recipient ID
            byte[] recipientId = ctx.getRecipientId();
            CBORObject connectionIdentifierInitiatorCbor = CBORObject.FromObject(recipientId);

            // Retrieve the EDHOC session associated to C_R and storing EDHOC message_3
            EdhocSessionPersistent session = edhocSessionsPersistent.get(connectionIdentifierInitiatorCbor);

            // Consistency checks
            if (session == null) {
                LOGGER.error("Unable to retrieve the EDHOC session");
                return;
            }

            if (!Arrays.equals(recipientId, session.getOscoreRecipientId())) {
                LOGGER.error("Inconsistent OSCORE recipient ids between OSCORE context and retrieved EDHOC session");
                return;
            }

            byte[] combinedMessage = createCombinedMessage(session.getMessage3(), request.getPayload(), ctx.getMaxUnfragmentedSize());
            request.setPayload(combinedMessage);
        }

        if(sendConcretizer != null) {
            request.addMessageObserver(new MessageObserverAdapter() {
                @Override
                public void onSent(boolean retransmission) {
                    if (!retransmission) {
                        sendConcretizer.concretize(request.getBytes());
                    }
                }
            });
        }

        super.sendRequest(exchange, request);
    }

    @Override
    public void sendResponse(Exchange exchange, Response response) {
        LOGGER.debug("Sending response through EDHOC layer");
        int MID = exchange.getRequest().getMID();
        EdhocSessionPersistent session = messageProcessorPersistent.getEdhocMapperState().getEdhocSessionPersistent();

        if (session.getCoapExchanger().getDraftQueue().removeIf(coapExInfo -> coapExInfo.getMID() == MID)) {
            // remove exchange from draft queue if left there and did not reach any resource
            // but encountered an error and response came from a middle layer, so
            // intercept that response, unset necessary exchange flags and
            // add it to received queue with Unsuccessful flag set
            exchange.getRequest().setAcknowledged(false);
            addUnsuccessfulCoapExchangeInfo(exchange, session);
            return;
        }

        if(sendConcretizer != null) {
            response.addMessageObserver(new MessageObserverAdapter() {
                @Override
                public void onSent(boolean retransmission) {
                    if (!retransmission) {
                        sendConcretizer.concretize(response.getBytes());
                    }
                }
            });
        }

        super.sendResponse(exchange, response);
    }

    @Override
    public void receiveRequest(Exchange exchange, Request request) {
        LOGGER.debug("Receiving request through EDHOC layer");

        if(recvConcretizer != null) {
            recvConcretizer.concretize(request.getBytes());
        }

        if (request.getOptions().hasEdhoc()) {

            if (!request.getOptions().hasOscore()) {
                LOGGER.error("Received a request with the EDHOC option but without the OSCORE option");
                return;
            }

            LOGGER.debug("Combined EDHOC+OSCORE request");
            LOGGER.debug(EdhocUtil.byteArrayToString("Received payload", request.getPayload()));

            CombinedMessagePair combinedMessagePair = splitCombinedMessage(request.getPayload());
            if (combinedMessagePair == null) {
                LOGGER.error("Could not split received combined message");
                return;
            }

            // Prepare the actual OSCORE request, by replacing the payload
            byte[] oscorePayload = combinedMessagePair.oscorePayload;
            LOGGER.debug(EdhocUtil.byteArrayToString("OSCORE request payload", oscorePayload));
            request.setPayload(oscorePayload);

            // Rebuild the full message_3 sequence
            CBORObject edhocMessage3 = combinedMessagePair.edhocMessage3;
            List<CBORObject> edhocObjectList = new ArrayList<>();

            // Find C_R, by encoding the 'kid' from the OSCORE option
            byte[] kid = getKid(request.getOptions().getOscore());
            CBORObject cR = messageProcessorPersistent.encodeIdentifier(kid);

            if (messageProcessorPersistent.getEdhocMapperState().receiveWithPrependedCX()) {
                // Prepend C_R if needed
                edhocObjectList.add(cR);
            }

            // Add EDHOC Message 3, the received CBOR byte string
            edhocObjectList.add(edhocMessage3);

            // Assemble the full EDHOC message_3
            byte[] message3Sequence = Util.buildCBORSequence(edhocObjectList);

            LOGGER.debug(EdhocUtil.byteArrayToString("Rebuilt EDHOC message_3", message3Sequence));

            CBORObject kidCbor = CBORObject.FromObject(kid);
            EdhocSessionPersistent session = edhocSessionsPersistent.get(kidCbor);

            // Consistency checks
            if (session == null) {
                LOGGER.error("Unable to retrieve the EDHOC session");
                return;
            }

            // Process EDHOC message_3
            boolean ok = messageProcessorPersistent.readMessage3(message3Sequence);

            if (ok) {
                addCoapExchangeInfo(request, true, session);
            } else {
                // message 3 could not be read successfully,
                // so do not propagate application data to upper layers
                // and register the received message as unsuccessful
                addUnsuccessfulCoapExchangeInfo(exchange, session);
                return;
            }
        } else {
            // edhoc message or application data or unknown message
            EdhocSessionPersistent session = messageProcessorPersistent.getEdhocMapperState().getEdhocSessionPersistent();

            if (request.getOptions().hasOscore()) {
                // wait in case message 3 is received and current OSCORE context
                // is about to be generated
                session.waitForOscoreContext(100);
                LOGGER.debug("Finished waiting for OSCORE context generation");
            }

            addCoapExchangeInfo(request, true, session);
        }

        super.receiveRequest(exchange, request);

    }

    @Override
    public void receiveResponse(Exchange exchange, Response response) {
        LOGGER.debug("Receiving response through EDHOC layer");

        if(recvConcretizer != null) {
            recvConcretizer.concretize(response.getBytes());
        }

        addCoapExchangeInfo(response, false,
                messageProcessorPersistent.getEdhocMapperState().getEdhocSessionPersistent());
        super.receiveResponse(exchange, response);
    }

    @Override
    public void sendEmptyMessage(Exchange exchange, EmptyMessage message) {
        super.sendEmptyMessage(exchange, message);
    }

    @Override
    public void receiveEmptyMessage(Exchange exchange, EmptyMessage message) {
        super.receiveEmptyMessage(exchange, message);
    }

    protected boolean hasCombinedMessageVersionEqV6() {
        CombinedMessageVersion version = messageProcessorPersistent.getEdhocMapperState().getCombinedMessageVersion();
        return version.equals(CombinedMessageVersion.v06);
    }

    protected byte[] createCombinedMessage(byte[] message3Sequence, byte[] oscorePayload, int maxUnfragmentedSize) {
        // Extract EDHOC message_3 from the stored CBOR sequence (? C_R, EDHOC message_3)
        CBORObject[] message3SequenceElements = CBORObject.DecodeSequenceFromBytes(message3Sequence);
        int index = messageProcessorPersistent.getEdhocMapperState().sendWithPrependedCX() ? 1 : 0;
        CBORObject edhocMessage3Cbor = message3SequenceElements[index];

        LOGGER.debug(EdhocUtil.byteArrayToString("Message 3 Sequence ", message3Sequence));
        LOGGER.debug(EdhocUtil.byteArrayToString("EDHOC Message 3", edhocMessage3Cbor.GetByteString()));
        LOGGER.debug(EdhocUtil.byteArrayToString("OSCORE payload", oscorePayload));

        byte[] combinedMessagePart1, combinedMessagePart2;

        if (hasCombinedMessageVersionEqV6()) {
            // The combined message is composed of two concatenated elements:
            // 1. A CBOR byte string, with value the EDHOC Message 3
            // 2. A CBOR byte string, with value the original OSCORE payload
            combinedMessagePart1 = CBORObject.FromObject(edhocMessage3Cbor.GetByteString()).EncodeToBytes();
            combinedMessagePart2 = CBORObject.FromObject(oscorePayload).EncodeToBytes();
        } else {
            // The combined message is composed of two concatenated elements:
            // 1. The EDHOC Message 3 (of type Byte String)
            // 2. The original OSCORE payload
            combinedMessagePart1 = edhocMessage3Cbor.EncodeToBytes();
            combinedMessagePart2 = oscorePayload;
        }

        int combinedMessageLength = combinedMessagePart1.length + combinedMessagePart2.length;

        // Abort if the payload of the EDHOC+OSCORE request exceeds MAX_UNFRAGMENTED_SIZE
        if (combinedMessageLength > maxUnfragmentedSize) {
            throw new IllegalStateException("The payload of the EDHOC+OSCORE request is exceeding MAX_UNFRAGMENTED_SIZE");
        }

        byte[] combinedMessage = new byte[combinedMessageLength];
        System.arraycopy(combinedMessagePart1, 0, combinedMessage, 0, combinedMessagePart1.length);
        System.arraycopy(combinedMessagePart2, 0, combinedMessage, combinedMessagePart1.length, combinedMessagePart2.length);

        LOGGER.debug(EdhocUtil.byteArrayToString("New OSCORE payload", combinedMessage));
        return combinedMessage;
    }

    protected static class CombinedMessagePair {
        public CBORObject edhocMessage3;
        public byte[] oscorePayload;

        public CombinedMessagePair(CBORObject edhocMessage3, byte[] oscorePayload) {
            this.edhocMessage3 = edhocMessage3;
            this.oscorePayload = oscorePayload;
        }
    }

    protected CombinedMessagePair splitCombinedMessage(byte[] combinedMessage) {
        if (combinedMessage == null) {
            LOGGER.error("Provided null combined message to split");
            return null;
        }

        if (hasCombinedMessageVersionEqV6()) {
            // CBOR objects included in the received CBOR sequence
            CBORObject[] cborObjectList = CBORObject.DecodeSequenceFromBytes(combinedMessage);

            if (cborObjectList == null || cborObjectList.length != 2) {
                LOGGER.error("Received CBOR Object List is null or has invalid length");
                return null;
            }

            if (cborObjectList[0].getType() != CBORType.ByteString
            || cborObjectList[1].getType() != CBORType.ByteString) {
                LOGGER.error("Received CBOR Objects have invalid type");
                return null;
            }

            return new CombinedMessagePair(cborObjectList[0], cborObjectList[1].GetByteString());
        }

        // version >= 7
        ByteArrayInputStream inputStream = new ByteArrayInputStream(combinedMessage);

        CBORObject edhocMessage3 = null;
        try {
            edhocMessage3 = CBORObject.Read(inputStream);
        } catch (CBORException e) {
            LOGGER.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }

        if (edhocMessage3 == null || edhocMessage3.getType() != CBORType.ByteString) {
            LOGGER.error("Invalid edhocMessage3 from received combined message");
            return null;
        }

        int oscoreLength = combinedMessage.length - edhocMessage3.EncodeToBytes().length;
        if (oscoreLength <= 0) {
            LOGGER.error("Negative or zero length of OSCORE message in received combined message");
            return null;
        }

        byte[] oscorePayload = new byte[oscoreLength];

        int bytesRead = -1;
        try {
            bytesRead = inputStream.read(oscorePayload, 0, oscoreLength);
        } catch (IndexOutOfBoundsException e) {
            LOGGER.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }

        if (bytesRead != oscoreLength) {
            return null;
        }

        return new CombinedMessagePair(edhocMessage3, oscorePayload);
    }

    protected void addCoapExchangeInfo(Message message, boolean isRequest,
                                       EdhocSessionPersistent edhocSessionPersistent) {
        if (message == null || edhocSessionPersistent == null) {
            return;
        }

        // prepare new coapExchangeInfo to add to session's coapExchanger queue
        CoapExchangeInfo coapExchangeInfo = new CoapExchangeInfo(message.getMID());

        if (message.getOptions().hasOscore()) {
            coapExchangeInfo.setHasOscoreAppMessage(true);

            if (message.getOptions().hasEdhoc()) {
                coapExchangeInfo.setHasEdhocMessage(true);
            }
        }

        // add coapExhangeInfo to appropriate queue
        if (isRequest) {
            // in case of request, add it to draft queue in order for server resources to
            // edit it and add it to received queue
            if (!edhocSessionPersistent.getCoapExchanger().getDraftQueue().offer(coapExchangeInfo)) {
                LOGGER.warn("Full draft queue found");
            }
        } else {
            // in case of response, add it to received queue immediately
            // for client to obtain
            if (!edhocSessionPersistent.getCoapExchanger().getReceivedQueue().offer(coapExchangeInfo)) {
                LOGGER.warn("Full received queue found");
            }
        }
    }

    protected void addUnsuccessfulCoapExchangeInfo(Exchange exchange, EdhocSessionPersistent edhocSessionPersistent) {
        int MID = exchange.getRequest().getMID();
        CoapExchangeInfo coapExchangeInfo = new CoapExchangeInfo(MID);
        coapExchangeInfo.setHasUnsuccessfulMessage(true);

        coapExchangeInfo.setCoapExchange(new CoapExchange(exchange));
        if (!edhocSessionPersistent.getCoapExchanger().getReceivedQueue().offer(coapExchangeInfo)) {
            LOGGER.warn("Full received queue found");
        }
    }

    /*
     * Returns the OSCORE Context that was used to protect this outgoing
     * exchange (outgoing request or response).
     */
    protected OSCoreCtx getContextForOutgoing(Exchange e) {

        String uri = e.getRequest().getURI();
        if (uri == null) {
            return null;
        }

        try {
            return ctxDb.getContext(uri);
        } catch (OSException exception) {
            LOGGER.error("Retrieving the OSCORE Security Context " + exception.getMessage());
            return null;
        }
    }

    /*
     * Retrieve KID value from an OSCORE option.
     */
    protected byte[] getKid(byte[] oscoreOption) {
        if (oscoreOption.length == 0) {
            return null;
        }

        // Parse the flag byte
        byte flagByte = oscoreOption[0];
        int n = flagByte & 0x07;
        int k = flagByte & 0x08;
        int h = flagByte & 0x10;

        byte[] kid = null;
        int index = 1;

        // Partial IV
        index += n;

        // KID Context
        if (h != 0) {
            int s = oscoreOption[index];
            index += s + 1;
        }

        // KID
        if (k != 0) {
            kid = Arrays.copyOfRange(oscoreOption, index, oscoreOption.length);
        }

        return kid;
    }
}
