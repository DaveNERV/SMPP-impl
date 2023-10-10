package org.example.server;

import org.jsmpp.SMPPConstant;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.jsmpp.PDUStringException;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.InterfaceVersion;
import org.jsmpp.session.BindRequest;
import org.jsmpp.session.SMPPServerSession;
import org.jsmpp.session.SMPPServerSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaitBindRequest implements Callable<Boolean> {

    private static final Logger log = LoggerFactory.getLogger(WaitBindRequest.class);
    private final SMPPServerSession serverSession;
    private final long timeout;
    private final String systemId;
    private final String password;

    public WaitBindRequest(SMPPServerSession serverSession, long timeout, String systemId, String password) {
        this.serverSession = serverSession;
        this.timeout = timeout;
        this.systemId = systemId;
        this.password = password;
    }

    @Override
    public Boolean call() {
        try {
            BindRequest bindRequest = serverSession.waitForBind(timeout);
            try {
                if (BindType.BIND_TRX.equals(bindRequest.getBindType())) {
                    if (systemId.equals(bindRequest.getSystemId())) {
                      if (password.equals(bindRequest.getPassword())) {
                            log.info("Accepting bind for session {}, interface version {}", serverSession.getSessionId(), bindRequest.getInterfaceVersion());

                            bindRequest.accept(systemId);
                            return true;
                        } else {
                            log.info("Rejecting bind for session {}, interface version {}, invalid password", serverSession.getSessionId(), bindRequest.getInterfaceVersion());
                            bindRequest.reject(SMPPConstant.STAT_ESME_RINVPASWD);
                        }
                    } else {
                        log.info("Rejecting bind for session {}, interface version {}, invalid system id", serverSession.getSessionId(), bindRequest.getInterfaceVersion());
                        bindRequest.reject(SMPPConstant.STAT_ESME_RINVSYSID);
                    }
                } else {
                    log.info("Rejecting bind for session {}, interface version {}, only accept transceiver", serverSession.getSessionId(), bindRequest.getInterfaceVersion());
                    bindRequest.reject(SMPPConstant.STAT_ESME_RBINDFAIL);
                }
            } catch (PDUStringException e) {
                log.error("Invalid system id: " + systemId, e);
                bindRequest.reject(SMPPConstant.STAT_ESME_RSYSERR);
            }

        } catch (IllegalStateException e) {
            log.error("System error", e);
        } catch (TimeoutException e) {
            log.warn("Wait for bind has reach timeout", e);
        } catch (IOException e) {
            log.error("Failed accepting bind request for session {}", serverSession.getSessionId());
        }
        return false;
    }
}
