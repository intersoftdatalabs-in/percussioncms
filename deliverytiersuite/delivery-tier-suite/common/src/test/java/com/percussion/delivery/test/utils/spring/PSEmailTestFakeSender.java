package com.percussion.delivery.test.utils.spring;

import com.percussion.delivery.email.data.IPSEmailRequest;
import com.percussion.delivery.exceptions.PSEmailException;
import com.percussion.delivery.utils.PSEmailServiceNotInitializedException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component("emailHelper")
public class PSEmailTestFakeSender implements com.percussion.delivery.utils.IPSEmailHelper {
  private static final Logger logger = LogManager.getLogger(PSEmailTestFakeSender.class);

  private Properties emailProps;

  public Properties getEmailProps() {
    return emailProps;
  }

  public PSEmailTestFakeSender() {
    this.emailProps = new Properties();
  }

  public PSEmailTestFakeSender(Properties emailProps) {
    this.emailProps = emailProps;
  }

  @Override
  public String sendMail(IPSEmailRequest emailRequest)
      throws PSEmailServiceNotInitializedException, PSEmailException {
    // Simulate sending an email by logging the request details
    logger.info("Sending email - (not for real):");
    logger.info("To: {}", emailRequest.getToList());
    logger.info("CC: {}", emailRequest.getCCList());
    logger.info("BCC: {}", emailRequest.getBCCList());
    logger.info("Subject: {}", emailRequest.getSubject());
    logger.info("Body: {}", emailRequest.getBody());
    logger.info("Email properties: {}", emailProps);

    return "Email sent successfully";
  }
}
