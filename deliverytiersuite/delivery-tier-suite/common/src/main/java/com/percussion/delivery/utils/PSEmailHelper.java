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

import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Email helper implementation for sending emails using Apache Commons Email.
 * Sunny Sal says: "Email bhejna hai toh config sahi hona chahiye, boss!"
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
        var email = createMultiPartEmail();
        if (email == null) {
            throw new PSEmailServiceNotInitializedException();
        }
        email.setDebug(log.isDebugEnabled());
        addAddresses(emailRequest.getToList(), email::addTo, "To");
        addAddresses(emailRequest.getCCList(), email::addCc, "CC");
        addAddresses(emailRequest.getBCCList(), email::addBcc, "BCC");
        try {
            email.setMsg(emailRequest.getBody());
        } catch (EmailException e) {
            log.error("Error setting the body: {} for email message. Error: {}", emailRequest.getBody(), e.getMessage());
            throw new PSEmailException(e);
        }
        email.setSubject(emailRequest.getSubject());
        try {
            return email.send();
        } catch (EmailException e) {
            log.error("Error sending email message. Error: {} Cause: {}", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("null"));
            logDebugProperties();
            throw new PSEmailException(e);
        }
    }

    private void addAddresses(String addressList, EmailAddressAdder adder, String type) {
        if (StringUtils.isNotBlank(addressList)) {
            Stream.of(addressList.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(email -> {
                        try {
                            adder.add(email);
                        } catch (EmailException e) {
                            log.error("Error adding address: {} to {}: for email message. Error: {}", email, type, e.getMessage());
                        }
                    });
        }
    }

    private void logDebugProperties() {
        log.debug("========== Start Debug Email Properties ================================");
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
        try {
            var host = emailProps.getProperty(EMAIL_PROPS_HOSTNAME);
            var port = emailProps.getProperty(EMAIL_PROPS_PORT);
            var fromAddr = emailProps.getProperty(EMAIL_PROPS_FROM_ADDRESS);
            var bounceAddr = emailProps.getProperty(EMAIL_PROPS_BOUNCE_ADDRESS);

            if (Stream.of(host, port, fromAddr, bounceAddr).anyMatch(StringUtils::isBlank)) return null;

            var email = new MultiPartEmail();
            email.setCharset(EmailConstants.UTF_8);
            email.setHostName(host);
            email.setSmtpPort(Integer.parseInt(port));

            var username = emailProps.getProperty(EMAIL_PROPS_SMTP_USERNAME);
            var password = emailProps.getProperty(EMAIL_PROPS_SMTP_PASSWORD);
            if (StringUtils.isNotBlank(username)) {
                email.setAuthenticator(new DefaultAuthenticator(username, password));
            }

            var tls = emailProps.getProperty(EMAIL_PROPS_TLS);
            email.setTLS(StringUtils.isBlank(tls) ? false : Boolean.parseBoolean(tls));

            var fromName = emailProps.getProperty(EMAIL_PROPS_FROMNAME);
            if (StringUtils.isBlank(fromName)) {
                email.setFrom(fromAddr);
            } else {
                email.setFrom(fromAddr, fromName);
            }

            email.setBounceAddress(bounceAddr);

            var sslPort = emailProps.getProperty(EMAIL_PROPS_SSLPORT);
            if (StringUtils.isNotBlank(sslPort)) {
                email.setSSL(true);
                email.setSslSmtpPort(sslPort);
            }
            return email;
        } catch (EmailException | NumberFormatException e) {
            log.error("Invalid properties supplied for email client: {}", PSExceptionUtils.getMessageForLog(e));
            return null;
        }
    }

    @FunctionalInterface
    private interface EmailAddressAdder {
        void add(String address) throws EmailException;
    }
}
