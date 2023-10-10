package org.example.client;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.SubmitSmResult;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageSubmit {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSubmit.class);

    private static final TimeFormatter TIME_FORMATTER = new AbsoluteTimeFormatter();

    private final String smppIp = "localhost";

    private int port = 8000;

    private final String systemId = "user";

    private final String password = "unifun";

    private final String sourceAddress = "1616";

    private static final String SERVICE_TYPE = "CMT";

    private static final Random RANDOM = new Random();

    public void broadcastMessage(String message, String destAddress, int count) {
        LOGGER.info("Broadcasting sms");
        SubmitSmResult result = null;

        int startIndex, endIndex, maxCharsPerSegment, totalSegments;
        OptionalParameter sarMsgRefNum, sarSegmentSeqnum, sarTotalSegments;

        SMPPSession session = initSession();

        maxCharsPerSegment = 254;
        totalSegments = (int)Math.ceil((double) message.length() / maxCharsPerSegment);
        sarTotalSegments = OptionalParameters.newSarTotalSegments(totalSegments);

        for(int i = 0; i < count; i++){
            sarMsgRefNum = OptionalParameters.newSarMsgRefNum((short)RANDOM.nextInt());

            try{
                for (int j = 0; j < totalSegments; j++) {
                    startIndex = j * maxCharsPerSegment;
                    endIndex = Math.min(startIndex + maxCharsPerSegment, message.length());

                    String segment = message.substring(startIndex, endIndex);

                    sarSegmentSeqnum = OptionalParameters.newSarSegmentSeqnum(j + 1);

                    result = submitMessage(session, segment, destAddress, sarMsgRefNum, sarSegmentSeqnum, sarTotalSegments);
                }
                LOGGER.info("Message submitted, result is {}", result);
                Thread.sleep(3000);
            } catch (NegativeResponseException | ResponseTimeoutException | PDUException | InvalidResponseException e) {
                LOGGER.error("Receive negative response", e);
            } catch (IOException e) {
                LOGGER.error("I/O error occured", e);
            } catch (Exception e) {
                LOGGER.error("Exception occured submitting SMPP request", e);
            }
            if(result != null) {
                LOGGER.info("Pushed message to broker successfully. Message: " + message);
            }else {
                LOGGER.info("Failed to push message to broker");
            }
        }
        if(session != null) session.unbindAndClose();
    }

    public SubmitSmResult submitMessage(SMPPSession session, String message, String destAddress,
                                       OptionalParameter sarMsgRefNum, OptionalParameter sarSegmentSeqnum, OptionalParameter sarTotalSegments)
            throws ResponseTimeoutException, PDUException, IOException, InvalidResponseException, NegativeResponseException {

        SubmitSmResult result = session.submitShortMessage(SERVICE_TYPE,
                TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, sourceAddress,
                TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, destAddress,
                new ESMClass(),
                (byte) 0,
                (byte) 1,
                TIME_FORMATTER.format(new Date()),
                null,
                new RegisteredDelivery().setSMSCDeliveryReceipt(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                (byte) 0,
                new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false),
                (byte) 0,
                message.getBytes(),
                sarMsgRefNum, sarSegmentSeqnum, sarTotalSegments
        );
        return result;
    }

    private SMPPSession initSession() {
        SMPPSession session = new SMPPSession();
        try {
            session.setMessageReceiverListener(new DeliverReceiverListenerImpl());
            String SMPPSystemId = session.connectAndBind(smppIp, port, new BindParameter(BindType.BIND_TRX, systemId, password, "cp", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN, null));
            LOGGER.info("Connected with SMPP with system id {}", SMPPSystemId);
        } catch (IOException e) {
            LOGGER.error("ESME was unable to connect to the SMPP session. Error occurred", e);
            session.close();
        }
        return session;
    }
}
