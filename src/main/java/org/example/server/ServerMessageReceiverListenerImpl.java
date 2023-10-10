package org.example.server;

import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.*;

import org.jsmpp.util.MessageId;
import org.jsmpp.util.RandomMessageIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jsmpp.bean.OptionalParameter.Tag;

public class ServerMessageReceiverListenerImpl implements ServerMessageReceiverListener {

    private static final Logger log = LoggerFactory.getLogger(ServerMessageReceiverListenerImpl.class);

    private final ExecutorService execServiceDelReceipt = Executors.newFixedThreadPool(100);

    private Queue<SARInfo> segmentedMessages = new LinkedList<>();

    @Override
    public SubmitSmResult onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source) throws ProcessRequestException{
        MessageId messageId = new RandomMessageIDGenerator().newMessageId();
        log.info("Receiving submit_sm '{}', and return message id {}", submitSm.getShortMessage(), messageId);

        byte[] sarMsgRefNum = submitSm.getOptionalParameter(Tag.SAR_MSG_REF_NUM).serialize();
        byte[] sarSegmentSeqnum = submitSm.getOptionalParameter(Tag.SAR_SEGMENT_SEQNUM).serialize();
        byte[] totalSegments = submitSm.getOptionalParameter(Tag.SAR_TOTAL_SEGMENTS).serialize();
        String message = new String(submitSm.getShortMessage(), StandardCharsets.UTF_8);

        if (sarMsgRefNum == null || sarSegmentSeqnum == null || totalSegments == null) {
            throw new ProcessRequestException("Optional Parameters missing.",  0x000000C3);
        }
        short refId = (short) (sarMsgRefNum[sarMsgRefNum.length - 1] & 0xFF);
        short segmentId = (short) (sarSegmentSeqnum[sarSegmentSeqnum.length - 1] & 0xFF);
        short segmentCount = (short) (totalSegments[totalSegments.length - 1] & 0xFF);

        if(isPresentOrIsNotFromTheSamePart(segmentId, refId)) {
            throw new ProcessRequestException("Invalid Optional Parameter Value", 0x000000C4);
        } else{
            if(isLastSegment(segmentCount, segmentId)) {
                segmentedMessages.add(new SARInfo(refId, segmentId, message));
                    log.info("Obtained message is: {}", assembleMessage());
            }else{
                segmentedMessages.add(new SARInfo(refId, segmentId, message));
            }
        }
        if (SMSCDeliveryReceipt.FAILURE.containedIn(submitSm.getRegisteredDelivery()) || SMSCDeliveryReceipt.SUCCESS_FAILURE.containedIn(submitSm.getRegisteredDelivery())) {
            execServiceDelReceipt.execute(new DeliverySmMessage(source, submitSm, messageId));
        }
        return new SubmitSmResult(messageId, new OptionalParameter[0]);
    }

    private boolean isLastSegment(short segmentCount, short segmentId) {
        return segmentCount == segmentId;
    }

    private boolean isPresentOrIsNotFromTheSamePart(short segmentId, short refId){
        for(SARInfo sarInfo : segmentedMessages){
            if(sarInfo.getSegmentId() == segmentId || sarInfo.getRefId() != refId){
                return true;
            }
        }
        return false;
    }

    private String assembleMessage(){
        StringBuilder assembledMessage = new StringBuilder();
        while(!segmentedMessages.isEmpty()) {
            assembledMessage.append(segmentedMessages.poll().getAssembledMessage());
        }
        return assembledMessage.toString();
    }

    @Override
    public SubmitMultiResult onAcceptSubmitMulti(SubmitMulti submitMulti, SMPPServerSession smppServerSession) throws ProcessRequestException {
        return null;
    }

    @Override
    public QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession smppServerSession) throws ProcessRequestException {
        return null;
    }

    @Override
    public void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession smppServerSession) throws ProcessRequestException {}

    @Override
    public void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession smppServerSession) throws ProcessRequestException {}

    @Override
    public BroadcastSmResult onAcceptBroadcastSm(BroadcastSm broadcastSm, SMPPServerSession smppServerSession) throws ProcessRequestException {
        return null;
    }

    @Override
    public void onAcceptCancelBroadcastSm(CancelBroadcastSm cancelBroadcastSm, SMPPServerSession smppServerSession) throws ProcessRequestException {}

    @Override
    public QueryBroadcastSmResult onAcceptQueryBroadcastSm(QueryBroadcastSm queryBroadcastSm, SMPPServerSession smppServerSession) throws ProcessRequestException {
        return null;
    }

    @Override
    public DataSmResult onAcceptDataSm(DataSm dataSm, Session session) throws ProcessRequestException {
        return null;
    }
}
