package org.example.server;

import java.util.Date;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.ServerResponseDeliveryAdapter;
import org.jsmpp.session.ServerResponseDeliveryListener;
import org.jsmpp.util.DeliveryReceiptState;
import org.jsmpp.util.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeliverySmMessage implements Runnable{
    private static final Logger log = LoggerFactory.getLogger(DeliverySmMessage.class);
    private final SMPPServerSession session;
    private final MessageId messageId;

    private final TypeOfNumber sourceAddrTon;
    private final NumberingPlanIndicator sourceAddrNpi;
    private final String sourceAddress;

    private final TypeOfNumber destAddrTon;
    private final NumberingPlanIndicator destAddrNpi;
    private final String destAddress;

    private final int totalSubmitted;

    private final int totalDelivered;

    private final String serviceType;

    private final byte[] shortMessage;

    //save data and reverse destination to source and source to destination
    public DeliverySmMessage(SMPPServerSession session,
                               SubmitSm submitSm, MessageId messageId) {
        this.session = session;
        this.messageId = messageId;

        sourceAddrTon = TypeOfNumber.valueOf(submitSm.getDestAddrTon());
        sourceAddrNpi = NumberingPlanIndicator.valueOf(submitSm.getDestAddrNpi());
        sourceAddress = submitSm.getDestAddress();

        destAddrTon = TypeOfNumber.valueOf(submitSm.getSourceAddrTon());
        destAddrNpi = NumberingPlanIndicator.valueOf(submitSm.getSourceAddrNpi());
        destAddress = submitSm.getSourceAddr();

        totalSubmitted = totalDelivered = 1;

        serviceType = submitSm.getServiceType();

        shortMessage = submitSm.getShortMessage();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
            Thread.currentThread().interrupt();
        }

        SessionState state = session.getSessionState();
        if (!state.isReceivable()) {
            log.debug("Not sending delivery_sm for message id {} since session state is {}", messageId, state);
            return;
        }

        String stringValue = messageId.getValue();
        try {
            DeliveryReceipt delRec = new DeliveryReceipt(stringValue, totalSubmitted, totalDelivered, new Date(), new Date(), DeliveryReceiptState.DELIVRD, "000", new String(shortMessage));
            session.deliverShortMessage(
                    serviceType,
                    sourceAddrTon, sourceAddrNpi, sourceAddress,
                    destAddrTon, destAddrNpi, destAddress,
                    new ESMClass(MessageMode.DEFAULT, MessageType.SMSC_DEL_RECEIPT, GSMSpecificFeature.DEFAULT),
                    (byte) 1,
                    (byte) 1,
                    new RegisteredDelivery(0),
                    DataCodings.ZERO,
                    delRec.toString().getBytes());
            log.debug("Sending delivery_sm for message id {}: {}", messageId, stringValue);
        } catch (Exception e) {
            log.error("Failed sending delivery_sm for message id " + messageId + ":" + stringValue, e);
        }
    }
}
