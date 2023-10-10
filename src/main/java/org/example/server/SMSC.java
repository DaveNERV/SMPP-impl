package org.example.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.PDUStringException;
import org.jsmpp.SMPPConstant;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.CancelSm;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.DataSm;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.InterfaceVersion;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.QuerySm;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.ReplaceSm;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.SubmitMulti;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.bean.UnsuccessDelivery;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.BindRequest;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.QuerySmResult;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPServerSessionListener;
import org.jsmpp.session.ServerMessageReceiverListener;
import org.jsmpp.session.ServerResponseDeliveryAdapter;
import org.jsmpp.session.Session;
import org.jsmpp.util.DeliveryReceiptState;
import org.jsmpp.util.MessageIDGenerator;
import org.jsmpp.util.MessageId;
import org.jsmpp.util.RandomMessageIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocketFactory;

public class SMSC extends ServerResponseDeliveryAdapter implements Runnable{

    private static final Logger log = LoggerFactory.getLogger(SMSC.class);

    private final ExecutorService execService = Executors.newFixedThreadPool(5);
    private final int port;
    private final String systemId;
    private final String password;
    public SMSC(int port, String systemId, String password) {
        this.port = port;
        this.systemId = systemId;
        this.password = password;
    }

    @Override
    public void run() {
        boolean running = true;

        try(SMPPServerSessionListener sessionListener = new SMPPServerSessionListener(port)) {
            sessionListener.setTimeout(60000);
            log.info("Listening on port {}", port);

            while (running) {
                SMPPServerSession serverSession = sessionListener.accept();
                log.info("Accepted connection with session {}", serverSession.getSessionId());
                serverSession.setMessageReceiverListener(new ServerMessageReceiverListenerImpl());

                Future<Boolean> bindResult = execService.submit(new WaitBindRequest(serverSession, 60000, systemId, password));
                try {
                    boolean bound = bindResult.get();
                    if (bound) {
                        log.info("The session is now in state {}", serverSession.getSessionState());
                    }
                } catch (InterruptedException e) {
                    log.info("Interrupted WaitBind task: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    running = false;
                } catch (ExecutionException e) {
                    log.info("Exception on execute WaitBind task: {}", e.getMessage());
                    running = false;
                }
            }
        } catch (IOException e) {
            log.error("IO error occurred", e);
        }
    }
}
