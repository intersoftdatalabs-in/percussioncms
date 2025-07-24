package com.percussion.delivery.utils;

import com.percussion.delivery.email.data.IPSEmailRequest;
import com.percussion.delivery.exceptions.PSEmailException;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Properties;

/**
 * Email helper implementation for sending emails using Apache Commons Email.
 * // REFACTORED: CP-JAVA11
 */
public class PSEmailHelper implements IPSEmailHelper {
    private final Properties emailProps;
    private static final Logger log = LogManager.getLogger(PSEmailHelper.class);

    public PSEmailHelper(Properties emailProps) {
        if (emailProps == null) {
            throw new IllegalArgumentException("emailProps must not be null");
        }
        this.emailProps = emailProps;
    }

    @Override
    public String sendMail(IPSEmailRequest emailRequest) throws PSEmailServiceNotInitializedException, PSEmailException {
        var commonsMultiPartEmail = createMultiPartEmail();
        if (commonsMultiPartEmail == null) {
            throw new PSEmailServiceNotInitializedException();
        }
        commonsMultiPartEmail.setDebug(log.isDebugEnabled());
        addAddresses(emailRequest.getToList(), commonsMultiPartEmail::addTo, "To");
        addAddresses(emailRequest.getCCList(), commonsMultiPartEmail::addCc, "CC");
        addAddresses(emailRequest.getBCCList(), commonsMultiPartEmail::addBcc, "BCC");
        try {
            commonsMultiPartEmail.setMsg(emailRequest.getBody());
        } catch (EmailException e) {
            log.error("Error setting the body: {} for email message. Error: {}", emailRequest.getBody(), e.getMessage());
            throw new PSEmailException(e);
        }
        commonsMultiPartEmail.setSubject(emailRequest.getSubject());
        try {
            return commonsMultiPartEmail.send();
        } catch (EmailException e) {
            log.error("Error sending email message. Error: {} Cause: {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "null");
            logDebugProperties();
            throw new PSEmailException(e);
        }
    }

    private void addAddresses(String addressList, EmailAddressAdder adder, String type) {
        if (StringUtils.isNotBlank(addressList)) {
            var emails = addressList.split(",");
            for (var email : emails) {
                try {
                    adder.add(email.trim());
                } catch (EmailException e) {
                    log.error("Error adding address: {} to {}: for email message. Error: {}", email, type, e.getMessage());
                }
            }
        }
    }

    private void logDebugProperties() {
        log.debug("========== Start Debug Email Properties ================================" );
        log.debug("SMTP User Name: {}", emailProps.getProperty(EMAIL_PROPS_SMTP_USERNAME));
        log.debug("SMTP User Password: {}", emailProps.getProperty(EMAIL_PROPS_SMTP_PASSWORD));
        log.debug("SMTP Host: {}", emailProps.getProperty(EMAIL_PROPS_HOSTNAME));
        log.debug("SMTP Port: {}", emailProps.getProperty(EMAIL_PROPS_PORT));
        log.debug("SMTP From Address: {}", emailProps.getProperty(EMAIL_PROPS_FROM_ADDRESS));
        log.debug("SMTP From Name: {}", emailProps.getProperty(EMAIL_PROPS_FROMNAME));
        log.debug("SMTP Bounce Address: {}", emailProps.getProperty(EMAIL_PROPS_BOUNCE_ADDRESS));
        log.debug("SMTP Use TLS: {}", emailProps.getProperty(EMAIL_PROPS_TLS));
        log.debug("SMTP SSL/TLS Port: {}", emailProps.getProperty(EMAIL_PROPS_SSLPORT));
        log.debug("============ End Debug Email Properties  ==============================");
    }

    private MultiPartEmail createMultiPartEmail() {
        MultiPartEmail commonsMultiPartEmail = null;
        try {
            var hostProp = emailProps.get(EMAIL_PROPS_HOSTNAME);
            if (StringUtils.isBlank((String) hostProp)) return null;
            var portProp = emailProps.get(EMAIL_PROPS_PORT);
            if (StringUtils.isBlank((String) portProp)) return null;
            var fromAddrProp = emailProps.get(EMAIL_PROPS_FROM_ADDRESS);
            if (StringUtils.isBlank((String) fromAddrProp)) return null;
            var bounceProp = emailProps.get(EMAIL_PROPS_BOUNCE_ADDRESS);
            if (StringUtils.isBlank((String) bounceProp)) return null;
            commonsMultiPartEmail = new MultiPartEmail();
            commonsMultiPartEmail.setCharset(EmailConstants.UTF_8);
            commonsMultiPartEmail.setHostName((String) hostProp);
            commonsMultiPartEmail.setSmtpPort(Integer.parseInt((String) portProp));
            if (!StringUtils.isBlank((String) emailProps.get(EMAIL_PROPS_SMTP_USERNAME))) {
                commonsMultiPartEmail.setAuthenticator(new DefaultAuthenticator(
                        (String) emailProps.get(EMAIL_PROPS_SMTP_USERNAME),
                        (String) emailProps.get(EMAIL_PROPS_SMTP_PASSWORD)));
            }
            if (!emailProps.containsKey(EMAIL_PROPS_TLS) || StringUtils.isBlank((String) emailProps.get(EMAIL_PROPS_TLS))) {
                commonsMultiPartEmail.setTLS(false);
            } else {
                commonsMultiPartEmail.setTLS(Boolean.parseBoolean((String) emailProps.get(EMAIL_PROPS_TLS)));
            }
            if (!emailProps.containsKey(EMAIL_PROPS_FROMNAME) || StringUtils.isBlank((String) emailProps.get(EMAIL_PROPS_FROMNAME))) {
                commonsMultiPartEmail.setFrom((String) fromAddrProp);
            } else {
                commonsMultiPartEmail.setFrom((String) fromAddrProp, (String) emailProps.get(EMAIL_PROPS_FROMNAME));
            }
            commonsMultiPartEmail.setBounceAddress((String) emailProps.get(EMAIL_PROPS_BOUNCE_ADDRESS));
            if (StringUtils.isNotBlank((String) emailProps.get(EMAIL_PROPS_SSLPORT))) {
                commonsMultiPartEmail.setSSL(true);
                commonsMultiPartEmail.setSslSmtpPort((String) emailProps.get(EMAIL_PROPS_SSLPORT));
            }
        } catch (EmailException e) {
            commonsMultiPartEmail = null;
            log.error("Invalid properties supplied for email client: {}", PSExceptionUtils.getMessageForLog(e));
        }
        return commonsMultiPartEmail;
    }

    @FunctionalInterface
    private interface EmailAddressAdder {
        void add(String address) throws EmailException;
    }
}
