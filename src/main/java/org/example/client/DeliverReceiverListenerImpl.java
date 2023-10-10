package org.example.client;

import org.jsmpp.bean.*;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;
import org.jsmpp.util.InvalidDeliveryReceiptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeliverReceiverListenerImpl implements MessageReceiverListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliverReceiverListenerImpl.class);

    public void onAcceptDeliverSm(DeliverSm deliverSm) {

        if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
            try {
                DeliveryReceipt delReceipt = deliverSm.getShortMessageAsDeliveryReceipt();

                LOGGER.info("Receiving delivery receipt for message '{}' from {} to {}: {}",
                        delReceipt.getId(), deliverSm.getSourceAddr(), deliverSm.getDestAddress(), delReceipt);
            } catch (InvalidDeliveryReceiptException e) {
                LOGGER.error("Failed getting delivery receipt", e);
            }
        }
    }

    public void onAcceptAlertNotification(AlertNotification alertNotification) {}

    public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) {
        return null;
    }
}
